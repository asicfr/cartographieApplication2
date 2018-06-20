It’s often convenient to develop applications using an in-memory embedded database. Obviously, 
in-memory databases do not provide persistent storage; you will need to populate your database when 
your application starts and be prepared to throw away data when your application ends.

DataSource configuration is controlled by external configuration properties in spring.datasource.*
. For example, you might declare the following section in application.properties:

spring.datasource.url=jdbc:mysql://localhost/test
spring.datasource.username=dbuser
spring.datasource.password=dbpass
spring.datasource.driver-class-name=com.mysql.jdbc.Driver
[Note]
You should at least specify the url using the spring.datasource.url property or Spring Boot will attempt 
to auto-configure an embedded database.
[Tip]
You often won’t need to specify the driver-class-name since Spring boot can deduce it for most databases from the url.
[Note]
For a pooling DataSource to be created we need to be able to verify that a valid Driver class is available, 
so we check for that before doing anything. I.e. if you set spring.datasource.driver-class-name=com.mysql.jdbc.Driver 
then that class has to be loadable.


Spring’s JdbcTemplate and NamedParameterJdbcTemplate classes are auto-configured and you can @Autowire 
them directly into your own beans: