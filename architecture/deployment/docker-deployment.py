from diagrams import Diagram, Cluster
from diagrams.c4 import Container, Database, SystemBoundary, Person, System, Relationship

with Diagram("E-Wallet - Production Deployment Architecture", show=False, filename="docker-deployment", direction="TB"):
    user = Person("User")

    with SystemBoundary("Prod Environment (prod)"):
        with SystemBoundary("Region: eu-west-1"):
            api_gw = System("API Gateway / WAF")
            oidc = System("OIDC Provider / IAM")
            secrets = System("Secrets Manager")
            kms = System("KMS / Encryption Keys")
            monitoring = System("Monitoring / Alerting")
            logging = System("Centralized Logging")
            backup = System("Backup / Object Storage")
            ci = System("CI/CD Pipeline")

            with SystemBoundary("Kubernetes Cluster"):
                ingress = Container("Ingress Controller")
                certmgr = Container("Cert Manager")

                with SystemBoundary("Namespace: ewallet-ns"):
                    api = Container("E-Wallet API (Spring Boot)")
                    db_replica = Database("Transaction Database (Replica)")

            with SystemBoundary("VM: dbHost"):
                db_primary = Database("Transaction Database (Primary)")

            # Access path
            user >> Relationship("Uses HTTPS") >> api_gw
            api_gw >> Relationship("Routes to") >> ingress
            ingress >> Relationship("Routes to") >> api

            # API to data and security services
            api >> Relationship("AuthN/AuthZ with") >> oidc
            api >> Relationship("Reads/Writes") >> db_primary
            db_primary >> Relationship("Streaming replication") >> db_replica

            # Secrets and encryption
            api >> Relationship("Retrieves secrets from") >> secrets
            secrets >> Relationship("Encrypts with") >> kms

            # TLS management
            ingress >> Relationship("TLS certs from") >> certmgr

            # Observability
            api >> Relationship("Emits metrics to") >> monitoring
            api >> Relationship("Sends logs to") >> logging
            db_primary >> Relationship("Emits metrics to") >> monitoring

            # Backup and recovery
            db_primary >> Relationship("Backups to") >> backup

            # CI/CD governance
            ci >> Relationship("Deploys to") >> api
            ci >> Relationship("Manages Ingress") >> ingress
            ci >> Relationship("Applies schema to") >> db_primary
