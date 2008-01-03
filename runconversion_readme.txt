The conversion script has been included in assignment post-2-4 branch now. You will need to run the conversion script first before starting your server with new Assignment post-2-4 code.

The purposes of doing the conversion are mainly:
1) remove existing duplicate submission records, if any;
2) prevent future submission duplicates by applying unique constraint on the ASSIGNMENT_SUBMISSION table;
3) improve performance of the Assignment tool;

Please take a look at the related JIRA SAK-11821 for more detailed descriptions about the conversion script. Basically, the conversion script does the following to your existing ASSIGNMENT_SUBMISSION table in Sakai database:
1) read in all tuples as AssignmentSubmission object, parse out data such as submitter_id, submit_time, submitted, and graded, and store those attributes as separate columns in the ASSIGNMENT_SUBMISSION table;
2) run though the table, combine and remove submission duplicates (tuples with same "context" and "submitter_id" combination);
3) apply the unique constraint of "context" + "submitter_id" to ASSIGNMENT_SUBMISSION table.

In order to compile the post-2-4 assignment module with your 2.4.x environment, you will need to:
1) have at least r38224 version of the 2.4.x util module. 
2) apply the patch included in SAK-12438 (OSP-assignments-post-2-4.patch) to your OSP 2.4.x module.

The following steps are required to run the conversion from 2.4 to 2.5 database schema for assignment (AssignmentService) to improve performance of the Assignment tool.  Unless otherwise  indicated, all files referred to are in the root directory of the assignment project. These instructions apply to MySQL or Oracle.  If a different database is used, a new version of the config file will be needed.

1) Edit the runconversion.sh and provide the path to the appropriate JDBC connector for your database.  Examples are shown for a MySQL driver in the local maven-2 repository or in the tomcat-commons-lib directory and for the Oracle driver in the tomcat-commons-lib directory.  If a different path is needed to find your driver, please add it to the CLASSPATH.
   
2) Edit the appropriate upgradeschema_*.config file for your database (either upgradeschema_mysql.config or upgradeschema_oracle.config) and supply datababase URL, username and password for your database where indicated:

		dbDriver=oracle.jdbc.driver.OracleDriver
		dbURL=PUT_YOUR_URL_HERE
		dbUser=PUT_YOUR_USERNAME_HERE
		dbPass=PUT_YOUR_PASSWORD_HERE

3) Run the conversion by running the shellscript and supplying the name of the config file as a parameter.  

	For example, to convert the database  schema for MySQL, the script would be invoked as follows:
   
		> ./runconversion.sh upgradeschema_mysql.config 
	
	To convert an Oracle database, the script would be invoked as follows:
   
		> ./runconversion.sh upgradeschema_oracle.config 
   
   