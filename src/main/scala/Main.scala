import java.nio.file.Files
import java.nio.file.Paths

import java.sql.DriverManager
import java.sql.Connection

import scala.util.{Try, Success, Failure}

// or "connection file" (which mysqldbcompare already uses), password not in test code/run script
case class DbServer(host: String, port: Int, user: String, pass: String) {
    def compareUrl = s"${user}:${pass}@${host}:${port}"
    
	// allowMultiQueries: needed for running "USE $name; ${file.text}"
    def jdbcUrl = s"jdbc:mysql://$host:$port?allowMultiQueries=true"
    def createConnection() = DriverManager.getConnection(jdbcUrl, user, pass)
}

sealed trait InputDb {
    def up(): Unit
    def down(): Unit
    def server: DbServer
    def dbName: String
}
case class RemoteDb(val server: DbServer, val dbName: String) extends InputDb {
    def up(){}
    def down(){}
}
case class LocalSql(server: DbServer, file: String) extends InputDb {
    import autoclose._
    // contents of folder, later.
    
    val uuid = java.util.UUID.randomUUID.toString
    val dbName = "TEMP_TEST_SCHEMA_" + uuid.replaceAll("-" , "_")
    
    // jdbc:mysql://user:pass@host:port[/dbName]
	
	def execNonQuery(stmt: java.sql.Statement, text: String) {
		println(s"${server.host}:${server.port} ==> $text")
		stmt.execute(text)
	}
    
    def up(){
        println(s"Up: $file")
		val contents = new String(Files.readAllBytes(Paths.get(file)))
        useC(server.createConnection()) { conn =>
          useC(conn.createStatement()) { stmt =>
            execNonQuery(stmt, s"CREATE DATABASE $dbName;")
			execNonQuery(stmt, s"USE $dbName; $contents")
          }
        }
        
    }
    def down(){
        println(s"Down: $file")
        useC(server.createConnection()) { conn =>
          useC(conn.createStatement()) { stmt =>
            execNonQuery(stmt, s"DROP DATABASE $dbName")
          }
        }
    }
} 

object App {
    def diffSchema(source: InputDb, target: InputDb) {
        // scala.sys.process.Process("cmd")
        // (for cmd line) -h host -u user (-p needs StdIn)
        // .run() vs .!, blocking
        // log to console & output & parser
        
        println(s"Source: ${source.server.compareUrl}/${source.dbName}")
		println(s"Target: ${target.server.compareUrl}/${target.dbName}")
        
        import scala.sys.process._
        // catch? "finally" for the outer ones works fine.
        Seq(
			"mysqldbcompare",
			s"--server1=${source.server.compareUrl}",
			s"--server2=${target.server.compareUrl}",
			"--run-all-tests",   // don't die just because the database names are different
			"--skip-data-check", // do not compare rows
			"--skip-row-count",  //well, maybe.
			//"--difftype xyz"
			//"--format xyz"
			s"${source.dbName}:${target.dbName}"
		).!
    }
    
    def main(args: Array[String]) {
        // --engine MYSQL|MSSQL|POSTGRES|...
        // --tempdb DbConnection
        // source: LocalSql or DbConnection
        // target: LocalSql or DbConnection
        // LocalSql: A folder or a .sql file.
        // DbConnection: user:pass@host:port or a file containing the same
        
        // JDBC. Could swap this out.
        val driver = "com.mysql.cj.jdbc.Driver"
        Class.forName(driver)
        
        
        
    
        val local = DbServer("localhost", 3306, "root", "admin")
        val tempDb = local
        
        val sourceDb = LocalSql(local, "schema/create/01_create.sql") 
        val targetDb = RemoteDb(local, "spark")
		//LocalSql(local, "schema/create/02_create.sql")
        
        sourceDb.up()
        try {
            targetDb.up()
            try {
                diffSchema(sourceDb, targetDb)
            } finally {
                targetDb.down()
            }
        } finally {
            sourceDb.down()
        }
    }
}

object autoclose {
    // scala doesn't have try-with-resources!
    def use[A, B](resource: A)(cleanup: A => Unit)(doWork: A => B): Try[B] = {
        try {
          Success(doWork(resource))
        } catch {
          case e: Exception => Failure(e)
        }
        finally {
          try {
            if (resource != null) {
              cleanup(resource)
            }
          } catch {
            case e: Exception => println(e) // should be logged
          }
        }
    }
	
	def useC[A <: java.lang.AutoCloseable, B](res: A)(doWork: A => B): Try[B] = use(res)(_.close)(doWork)
}