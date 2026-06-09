# ResumeForge Environment Variables Setup Guide

## ✅ PHASE 1.1 FIX: Critical Secrets Management

All hardcoded credentials have been removed. Services now require explicit environment variable configuration.

---

## Required Environment Variables

### Database Configuration (CRITICAL ⚠️)

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `DB_HOST` | localhost | PostgreSQL host | `resumeforge-db.rds.amazonaws.com` |
| `DB_PORT` | 5432 | PostgreSQL port | `5432` |
| `DB_NAME` | resumeforge | Database name | `resumeforge` |
| `DATABASE_USERNAME` | resumeforge | Database user | `postgres` |
| `DATABASE_PASSWORD` | **NOT_SET_IN_ENV** | Database password (MUST OVERRIDE) | `your-secure-password-here` |

**⚠️ CRITICAL:** `DATABASE_PASSWORD` has NO safe default. Must be set in production environment.

---

### JWT & Authentication

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `JWT_SECRET` | **NOT_SET_IN_ENV_MUST_BE_32_CHARS_MIN** | JWT signing key (min 32 chars) | `your-256-bit-secret-key-here-min32chars` |

**⚠️ CRITICAL:** JWT_SECRET must be 32+ characters and unique per environment.

---

### Service Routing (API Gateway)

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `RESUME_SERVICE_URL` | `http://localhost:8081` | Resume service endpoint | `http://resume-service:8081` (Docker) |
| `WORKER_SERVICE_URL` | `http://localhost:8082` | Worker service endpoint | `http://worker-service:8082` (Docker) |

---

### Redis Configuration

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `REDIS_HOST` | localhost | Redis host | `redis.resumeforge.svc.cluster.local` (K8s) |
| `REDIS_PORT` | 6379 | Redis port | `6379` |
| `REDIS_URL` | `redis://localhost:6379` | Full Redis URL (alternative to HOST:PORT) | `redis://:password@redis-host:6379` |

---

### Kafka Configuration

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka brokers | `kafka1:9092,kafka2:9092,kafka3:9092` |
| `KAFKA_SECURITY_PROTOCOL` | PLAINTEXT | Security protocol | `SASL_SSL` (production) |
| `KAFKA_SASL_MECHANISM` | PLAIN | SASL mechanism | `PLAIN` |
| `KAFKA_SASL_JAAS_CONFIG` | (empty) | JAAS configuration | `org.apache.kafka.common.security.plain.PlainLoginModule required username="user" password="pass";` |
| `KAFKA_CA_CERT` | (empty) | CA certificate (for SSL) | Certificate content or path |

---

### LLM & Ollama

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `OLLAMA_API_URL` | `https://ollama.com/v1/chat/completions` | Ollama API endpoint | `http://ollama:11434/v1/chat/completions` |
| `OLLAMA_MODEL` | gemma3:12b | Model name | `llama2`, `mistral`, etc. |
| `OLLAMA_API_KEY` | (empty) | API key if required | API key for managed Ollama |

---

### CORS & Frontend

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:3001` | Allowed origins | `https://resumeforge.vercel.app,https://www.resumeforge.app` |

---

### Logging

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `LOG_LEVEL` | INFO | Log level | `DEBUG`, `INFO`, `WARN`, `ERROR` |

---

### Server Ports

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | Service-specific (8080, 8081, 8082) | Server port |

---

## Setup Instructions by Environment

### 1. Local Development

Create `.env` file in project root:

```bash
# .env (NEVER commit this file!)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=resumeforge
DATABASE_USERNAME=resumeforge
DATABASE_PASSWORD=your-local-db-password
JWT_SECRET=your-local-jwt-secret-32-chars-minimum-here
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
REDIS_HOST=localhost
REDIS_PORT=6379
OLLAMA_API_URL=http://localhost:11434/v1/chat/completions
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001
LOG_LEVEL=DEBUG
```

Load with Spring Boot:
```bash
# IDE or command line
export $(cat .env | xargs)
mvn spring-boot:run

# Or pass as properties
mvn spring-boot:run -Dspring-boot.run.arguments="--DATABASE_PASSWORD=password --JWT_SECRET=secret"
```

---

### 2. Docker Compose

Create `docker-compose.yml`:

```yaml
version: '3.9'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: resumeforge
      POSTGRES_USER: resumeforge
      POSTGRES_PASSWORD: your-secure-db-password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  api-gateway:
    build:
      context: .
      dockerfile: api-gateway/Dockerfile
    environment:
      PORT: 8080
      RESUME_SERVICE_URL: http://resume-service:8081
      WORKER_SERVICE_URL: http://worker-service:8082
      REDIS_HOST: redis
      REDIS_PORT: 6379
    ports:
      - "8080:8080"
    depends_on:
      - redis

  resume-service:
    build:
      context: .
      dockerfile: resume-service/Dockerfile
    environment:
      PORT: 8081
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: resumeforge
      DATABASE_USERNAME: resumeforge
      DATABASE_PASSWORD: your-secure-db-password
      JWT_SECRET: your-jwt-secret-32-chars-minimum-here
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      REDIS_URL: redis://redis:6379
      OLLAMA_API_URL: http://ollama:11434/v1/chat/completions
      OLLAMA_MODEL: gemma3:12b
    ports:
      - "8081:8081"
    depends_on:
      - postgres
      - kafka
      - redis

  worker-service:
    build:
      context: .
      dockerfile: worker-service/Dockerfile
    environment:
      PORT: 8082
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: resumeforge
      DATABASE_USERNAME: resumeforge
      DATABASE_PASSWORD: your-secure-db-password
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      OLLAMA_API_URL: http://ollama:11434/v1/chat/completions
      OLLAMA_MODEL: gemma3:12b
    ports:
      - "8082:8082"
    depends_on:
      - postgres
      - kafka

  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama

volumes:
  postgres_data:
  ollama_data:
```

Run:
```bash
docker-compose up
```

---

### 3. Kubernetes Deployment

Create `resumeforge-secrets.yaml`:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: resumeforge-secrets
  namespace: resumeforge
type: Opaque
stringData:
  DATABASE_PASSWORD: your-secure-db-password
  JWT_SECRET: your-jwt-secret-32-chars-minimum-here
  KAFKA_SASL_JAAS_CONFIG: "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kafka-user\" password=\"kafka-password\";"
```

Create `resume-service-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: resume-service
  namespace: resumeforge
spec:
  replicas: 3
  selector:
    matchLabels:
      app: resume-service
  template:
    metadata:
      labels:
        app: resume-service
    spec:
      containers:
      - name: resume-service
        image: resumeforge/resume-service:latest
        ports:
        - containerPort: 8081
        env:
        - name: PORT
          value: "8081"
        - name: DB_HOST
          value: postgres.resumeforge.svc.cluster.local
        - name: DB_PORT
          value: "5432"
        - name: DB_NAME
          value: resumeforge
        - name: DATABASE_USERNAME
          value: resumeforge
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: resumeforge-secrets
              key: DATABASE_PASSWORD
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: resumeforge-secrets
              key: JWT_SECRET
        - name: KAFKA_BOOTSTRAP_SERVERS
          value: kafka.resumeforge.svc.cluster.local:9092
        - name: KAFKA_SECURITY_PROTOCOL
          value: SASL_SSL
        - name: KAFKA_SASL_MECHANISM
          value: PLAIN
        - name: KAFKA_SASL_JAAS_CONFIG
          valueFrom:
            secretKeyRef:
              name: resumeforge-secrets
              key: KAFKA_SASL_JAAS_CONFIG
        - name: REDIS_HOST
          value: redis.resumeforge.svc.cluster.local
        - name: REDIS_PORT
          value: "6379"
        - name: OLLAMA_API_URL
          value: http://ollama.resumeforge.svc.cluster.local:11434/v1/chat/completions
        - name: OLLAMA_MODEL
          value: gemma3:12b
        - name: CORS_ALLOWED_ORIGINS
          value: https://resumeforge.vercel.app
        - name: LOG_LEVEL
          value: INFO
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 10
          periodSeconds: 5
```

Deploy:
```bash
kubectl apply -f resumeforge-secrets.yaml
kubectl apply -f resume-service-deployment.yaml
```

---

### 4. AWS Elastic Beanstalk

Create `.ebextensions/environment-variables.config`:

```yaml
option_settings:
  aws:autoscaling:launchconfiguration:
    SecurityGroups: resumeforge-app-sg
  aws:elasticbeanstalk:application:environment:
    DB_HOST: resumeforge-db.rds.amazonaws.com
    DB_PORT: 5432
    DB_NAME: resumeforge
    DATABASE_USERNAME: resumeforge
    KAFKA_BOOTSTRAP_SERVERS: b-1.resumeforge-msk.1a2b3c.kafka.us-east-1.amazonaws.com:9092
    KAFKA_SECURITY_PROTOCOL: SASL_SSL
    KAFKA_SASL_MECHANISM: PLAIN
    REDIS_HOST: resumeforge-redis.xxxxxxx.ng.0001.use1.cache.amazonaws.com
    REDIS_PORT: 6379
    OLLAMA_API_URL: https://ollama-api.resumeforge.com/v1/chat/completions
    OLLAMA_MODEL: gemma3:12b
    CORS_ALLOWED_ORIGINS: https://resumeforge.vercel.app
    LOG_LEVEL: INFO
```

Store secrets in AWS Secrets Manager:
```bash
aws secretsmanager create-secret \
  --name resumeforge/db-password \
  --secret-string "your-secure-db-password"

aws secretsmanager create-secret \
  --name resumeforge/jwt-secret \
  --secret-string "your-jwt-secret-32-chars-minimum-here"
```

Reference in `.ebextensions`:
```yaml
Resources:
  EBIAMRolePolicy:
    Type: AWS::IAM::Policy
    Properties:
      PolicyDocument:
        Version: '2012-10-17'
        Statement:
          - Effect: Allow
            Action:
              - secretsmanager:GetSecretValue
            Resource: arn:aws:secretsmanager:region:account:secret:resumeforge/*
```

---

### 5. GitHub Actions / CI/CD

Add to `.github/workflows/deploy.yml`:

```yaml
name: Deploy to Elastic Beanstalk

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build with Maven
        run: mvn clean package -DskipTests

      - name: Deploy to Elastic Beanstalk
        env:
          AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
          AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
          JWT_SECRET: ${{ secrets.JWT_SECRET }}
        run: |
          pip install awsebcli
          eb init -p docker resumeforge-app --region us-east-1
          eb deploy \
            --envvars \
            DATABASE_PASSWORD=$DB_PASSWORD,\
            JWT_SECRET=$JWT_SECRET
```

Add secrets to GitHub repository settings:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `DB_PASSWORD`
- `JWT_SECRET`

---

## Security Best Practices

### ✅ DO:
- Use environment variables for all sensitive data
- Use AWS Secrets Manager, Google Secret Manager, or HashiCorp Vault for production
- Rotate secrets regularly (at least quarterly)
- Use different secrets per environment (dev, staging, prod)
- Never commit `.env` files to git
- Add `.env` to `.gitignore`

### ❌ DON'T:
- Hardcode secrets in configuration files
- Use weak default values (like `resumeforge123`)
- Store secrets in source code
- Use the same secret across environments
- Log sensitive information
- Share secrets via email or Slack

---

## Troubleshooting

### Error: "Could not establish connection" (Database)
- **Check:** `DATABASE_PASSWORD` is correctly set
- **Check:** DB host is reachable from the service
- **Check:** Database credentials are correct

### Error: "JWT secret not set" 
- **Check:** `JWT_SECRET` is at least 32 characters
- **Check:** Environment variable is exported

### Error: "Cannot connect to Kafka"
- **Check:** `KAFKA_BOOTSTRAP_SERVERS` is correct
- **Check:** Kafka security protocol matches your setup (PLAINTEXT for dev, SASL_SSL for prod)
- **Check:** SASL credentials are correct

### Error: "Unknown host: resume-service"
- **Check:** `RESUME_SERVICE_URL` uses correct hostname (localhost for dev, service DNS for Docker/K8s)
- **Check:** Services are running and network is configured

---

## Next Steps

1. **Update your deployment workflows** to use this guide
2. **Add secret rotation policy** for production
3. **Audit existing logs** for sensitive data exposure
4. **Set up monitoring** for failed authentication attempts

---

*Phase 1.1 Complete: All hardcoded credentials removed*  
*Generated: 2026-06-08*
