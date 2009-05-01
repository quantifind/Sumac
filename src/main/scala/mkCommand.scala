package optional

object MakeCommand
{
  val template = """
_%s() 
{
    local cur prev opts
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    opts="%s"

    if [[ ${cur} == -* ]] ; then
        COMPREPLY=( $(compgen -W "${opts}" -- ${cur}) )
        return 0
    fi
}
complete -F _%s %s
alias %s='scala %s $*'
  """
  def mkTemplate(name: String, className: String, opts: Seq[String]): String =
    template.format(name, opts mkString " ", name, name, name, className)
  
  private def getArgNames(className: String) = {
    val clazz = Class.forName(className + "$")
    val singleton = clazz.getField("MODULE$").get()
    val m = clazz.getMethod("argumentNames")
    
    (m invoke singleton).asInstanceOf[Array[String]] map ("--" + _)
  }    
  
  def _main(args: Array[String]): Unit = {
    if (args == null || args.size != 2)
      return println("Usage: mkCommand <name> <class>")
      
    val Array(scriptName, className) = args
    val opts = getArgNames(className)

    val txt = mkTemplate(scriptName, className, opts)
    val tmpfile = java.io.File.createTempFile(scriptName, "", null)
    val writer = new java.io.FileWriter(tmpfile)
    writer write txt
    writer.close()
    
    println("# run this command in bash")
    println("source " + tmpfile.getAbsolutePath())
  }
}