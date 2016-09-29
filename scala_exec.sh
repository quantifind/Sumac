#!/bin/bash

script_dir="$(cd "$(dirname ${BASH_SOURCE[0]})" && pwd)"
[ -f "${script_dir}/import_envar.sh" ] && source "${script_dir}/import_envar.sh"

JAVA_OPTS+=" -Xmx32g -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=512m -XX:ReservedCodeCacheSize=128m"
JAVA_OPTS+=" -Djava.library.path=${script_dir}/lmdbjni/native/osx64"

# By default, use the root project
proj_name="." 

function log_info { >&2 echo "[INFO]: $@"; }
function log_warn { >&2 echo "[WARNING]: $@"; }
function quit_with { >&2 echo "[QUIT]: $@"; exit 2; }
function help() {
    cat << _HELP_EOF_
usage: $(basename ${BASH_SOURCE[0]})
 
options:
          -h  print this help
          -p <proj>  project name
          -f <file>  execute a scala script
          -e <expr>  execute a scala expression
          -c <class> run a class with optional arguments
                     pass additional args after '--'
_HELP_EOF_
    exit 1
}

while getopts ":e:f:p:c:bh" OPT; do
    case "$OPT" in
    p) log_info "project: $OPTARG"       
       proj_name="$OPTARG"
       [ -d "${script_dir}/${proj_name}/" ] || \
           quit_with "project directory ${proj_name} must exist"
       ;;
	b) log_info "building the package"
	   shift; log_info "args passed to sbt: ${@}"	   
	   ( cd "${script_dir}" && ./sbt/sbt_exec.sh "${@}")
	   exit
	   ;;
	f) log_info "running scala script: $OPTARG"
	   scala_file="$OPTARG"
	   ;;
	c) log_info "running scala class: $OPTARG"
	   scala_class_name="$OPTARG"
	   ;;
	e) log_info "executing scala expression: $OPTARG"
	   tmp_fname=$(mktemp /tmp/scala_expr_script.XXXXXX)
	   cat << _EXPR_EOF_ > ${tmp_fname}
$OPTARG 
_EXPR_EOF_
	   scala_file="${tmp_fname}"
	   ;;
    h) help 
       ;;
	# \?) log_warn "unknown option -$OPTARG"
    #     ;;
    esac
done
shift $((OPTIND - 1))

if [ ! -d "${script_dir}/target/pack/" ]; then
    log_info "repacking"
    ( cd "${script_dir}" && ./sbt/sbt_exec.sh "pack")
fi

log_warn "including joda or nscala time with qfish is likely to confuse Jackson"
extra_jars=($(find "${script_dir}/target/pack/lib" -name '*.jar' -and \
                   -not \( -name 'joda-*.jar' -or -name 'nscala-*.jar' \) -type f))

[ -f QFISH_JARS ] || ( cd "${script_dir}" && ./sbt/sbt_exec.sh 'printQfishJars' )


qfish_jars=()
qfish_jars_dir="${script_dir}/${proj_name}/target/pack/qfish/lib"
mkdir -p "${qfish_jars_dir}"
for jar_fname in $(cat QFISH_JARS | tr ':' '\n'); do
    if [ -f "${jar_fname}" ]; then
        tgt_jar="${qfish_jars_dir}/$(basename "${jar_fname}")"
        if [ ! -f "${tgt_jar}" ]; then
            log_info "(dry-run) copying ${jar_fname} to ${tgt_jar}"
            cp "${jar_fname}" "${tgt_jar}"
        fi
        #qfish_jars+=("${jar_fname}")
        qfish_jars+=("${tgt_jar}")
    fi
done

function join { local IFS="$1"; shift; echo "$*"; }
extra_classpaths="$(join ":" ${extra_jars[@]}):$(join ":" ${qfish_jars[@]})"

pushd "${proj_name}"

#( cd "${script_dir}" && ./sbt/sbt_exec.sh 'test:console' )

#exec java -cp "${extra_classpaths}" scala.tools.nsc.MainGenericRunner "${scala_file}"
export JAVA_OPTS="${JAVA_OPTS}"
if [ -n "${scala_file}" ]; then
    exec java ${JAVA_OPTS} -cp "${extra_classpaths}" "com.qf.quantodian.REPL" --fileName "${scala_file}"
elif [ -n "${scala_class_name}" ]; then
    echo "options $@"
    echo "java options: ${JAVA_OPTS}"

    exec_script="target/pack/${scala_class_name##*.}"
    echo "generate command to script ${exec_script}"
    cat << _SCRIPT_ENV_EOF_ > "${exec_script}"
#!/bin/bash
JAVA_OPTS+="${JAVA_OPTS}"
scala_class_name="${scala_class_name}"
_SCRIPT_ENV_EOF_

cat << '_SCRIPT_EOF_' >> "${exec_script}"
_bsd_="$(cd "$(dirname ${BASH_SOURCE[0]})" && pwd)"
pushd ${_bsd_}

extra_jars=($(find "${_bsd_}/lib" -name '*.jar' -and \
                   -not \( -name 'joda-*.jar' -or -name 'nscala-*.jar' \) -type f))

function join { local IFS="$1"; shift; echo "$*"; }
extra_classpaths="$(join ":" ${extra_jars[@]})"

if [ -d "${_bsd_}/qfish/lib/" ]; then
   qfish_jars=($(find "${_bsd_}/qfish/lib" -name '*.jar'))
   extra_classpaths+=":$(join ":" ${qfish_jars[@]})"
fi

if [[ $# -gt 0 ]]; then args="-- $@"; fi
exec java ${JAVA_OPTS} -cp "${extra_classpaths}" "${scala_class_name}" "$args"

popd
_SCRIPT_EOF_

    chmod +x "${exec_script}"
    exec ${exec_script}
else
    exec java ${JAVA_OPTS} -cp "${extra_classpaths}" "com.qf.quantodian.REPL"
fi
