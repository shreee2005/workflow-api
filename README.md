# workflow-api

Spring Boot backend for WorkFlow project.

## Quick start (local, without docker)
1. Configure `src/main/resources/application-dev.yml` or environment variables:
    - SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/workflow_dev
    - SPRING_DATASOURCE_USERNAME=wf_user
    - SPRING_DATASOURCE_PASSWORD=wf_pass
    - SPRING_RABBITMQ_HOST=localhost

2. Build and run:
   ```bash
   ./mvnw clean package
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

3 . Start
.\scripts\start-api.ps1
