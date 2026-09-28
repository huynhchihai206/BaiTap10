-- Run in SSMS on your local SQL Server instance.
USE master;
GO
IF DB_ID(N'jwt_nimbus') IS NULL
    CREATE DATABASE jwt_nimbus;
GO
