import java.nio.file.Files
import java.nio.file.Paths

// or "connection file" (which mysqldbcompare already uses), password not in test code/run script
case class DbServer(host: String, port: Int, user: String, pass: String) {
	def compare = s"${user}:${pass}@${host}:${port}"
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
	// contents of folder, later.
	
	val uuid = java.util.UUID.randomUUID.toString
	val dbName = "TEMP_TEST_SCHEMA_" + uuid.replaceAll("-" , "_")
	
	// jdbc:mysql://user:pass@host:port/dbName
	// dbName?allowMultiQueries=true - needed for running "USE $name; ${file.text}"
	
	def up(){
		// CREATE DATABASE $name; USE $name; ${file.text}
		println(s"Up: $dbName")
		val contents = new String(Files.readAllBytes(Paths.get(file)))
		println(contents)
	}
	def down(){
		// - down: (new connection) DROP DATABASE $name
		println(s"Down: $dbName")
	}
} 

object App {
	def diffSchema(source: InputDb, target: InputDb) {
		// scala.sys.process.Process("cmd")
		// (for cmd line) -h host -u user (-p needs StdIn)
		// .run() vs .!, blocking
		// log to console & output & parser
		

		println(s"Compare: ${source.server.compare}/${source.dbName} to ${target.server.compare}/${target.dbName}")
	}
	
	def main(args: Array[String]) {
		// --engine MYSQL|MSSQL|POSTGRES|...
		// --localdb *connection*
		// source target
	
		val local = DbServer("localhost", 3306, "root", "admin")
		val tempDb = local
		
		val sourceDb = LocalSql(local, "schema/create/01_create.sql") 
		val targetDb = LocalSql(local, "schema/create/02_create.sql")
		
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