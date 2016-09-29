#!/bin/bash

SBT_VER=0.13.11
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

set -ex

if [ ! -x ${script_dir}/bin/sbt ]; then (
    cd ${script_dir}
    tarball=sbt-${SBT_VER}.tgz
    [ -f "${tarball}" ] || curl -LO "https://dl.bintray.com/sbt/native-packages/sbt/${SBT_VER}/${tarball}"
    tar -zxvf "${tarball}" --strip-components=1
    [ -x ./bin/sbt ] || chmod +x ./bin/sbt
) fi

export SBT_OPTS="$SBT_OPTS -Dfile.encoding=UTF8"
export JAVA_OPTS="${JAVA_OPTS} -Xmx32G -XX:+CMSClassUnloadingEnabled -XX:ReservedCodeCacheSize=128m"
exec ${script_dir}/bin/sbt -v "$@"
