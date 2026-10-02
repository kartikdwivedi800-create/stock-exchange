-- Runs once, the first time the postgres container starts with an EMPTY data volume.
-- One database + one login role per service (ADR-0003). A service's role can only
-- connect to its own database, so services cannot read each other's tables.
-- Dev-only passwords (same as the role name); real ones come from secrets in k8s.

-- user-service
CREATE ROLE user_service LOGIN PASSWORD 'user_service';
CREATE DATABASE user_db OWNER user_service;
REVOKE ALL ON DATABASE user_db FROM PUBLIC;

-- wallet-service
CREATE ROLE wallet_service LOGIN PASSWORD 'wallet_service';
CREATE DATABASE wallet_db OWNER wallet_service;
REVOKE ALL ON DATABASE wallet_db FROM PUBLIC;

-- order-service
CREATE ROLE order_service LOGIN PASSWORD 'order_service';
CREATE DATABASE order_db OWNER order_service;
REVOKE ALL ON DATABASE order_db FROM PUBLIC;

-- portfolio-service
CREATE ROLE portfolio_service LOGIN PASSWORD 'portfolio_service';
CREATE DATABASE portfolio_db OWNER portfolio_service;
REVOKE ALL ON DATABASE portfolio_db FROM PUBLIC;

-- stock-service
CREATE ROLE stock_service LOGIN PASSWORD 'stock_service';
CREATE DATABASE stock_db OWNER stock_service;
REVOKE ALL ON DATABASE stock_db FROM PUBLIC;

-- matching-service
CREATE ROLE matching_service LOGIN PASSWORD 'matching_service';
CREATE DATABASE matching_db OWNER matching_service;
REVOKE ALL ON DATABASE matching_db FROM PUBLIC;

-- notification-service
CREATE ROLE notification_service LOGIN PASSWORD 'notification_service';
CREATE DATABASE notification_db OWNER notification_service;
REVOKE ALL ON DATABASE notification_db FROM PUBLIC;
