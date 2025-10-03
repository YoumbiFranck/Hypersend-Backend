Database Configuration with pgAdmin
Acces to Database: http://localhost:5051/browser/

Name = `PostgreSQL Master`
    - Host: `hps_postgres_master`
    - Port: `5432`
    - Database: `hypersend`
    - Username: `hypersend_user`
    - Password: `hypersend_password`


Name = `PostgreSQL Slave`
    - Host: `hps_postgres_slave`
    - Port: `5432`

Load Balancer and HAProxy Stats:
http://localhost:8404/stats // HAProxy Stats (admin/hypersend) --> see haproxy.cfg for details

Eureka Dashboard:
http://localhost:8761/

