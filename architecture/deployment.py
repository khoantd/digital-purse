from diagrams import Diagram, Cluster
from diagrams.c4 import Person, System, Container, Database, SystemBoundary, Relationship

with Diagram("E-Wallet Deployment Architecture", show=False, filename="deployment", direction="TB"):
    user = Person("User")

    with SystemBoundary("E-Wallet System"):
        apiService = Container("E-Wallet API", "Spring Boot")
        transactionDb = Database("Transaction Database", "PostgreSQL")

    user >> Relationship("interacts with") >> apiService
    apiService >> Relationship("stores transaction data") >> transactionDb
