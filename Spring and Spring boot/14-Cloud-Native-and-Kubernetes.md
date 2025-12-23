# Cloud Native & Kubernetes with Spring Boot

## Table of Contents
1. [Kubernetes-Native Spring Boot](#kubernetes-native-spring-boot)
2. [Liveness and Readiness Probes](#liveness-and-readiness-probes)
3. [Externalized Configuration (ConfigMaps & Secrets)](#externalized-configuration-configmaps--secrets)
4. [Graceful Shutdown & Lifecycle](#graceful-shutdown--lifecycle)
5. [Service Discovery & Load Balancing in K8s](#service-discovery--load-balancing-in-k8s)
6. [Helm Charts for Spring Boot](#helm-charts-for-spring-boot)

---

## Kubernetes-Native Spring Boot
Spring Boot is "Kubernetes-aware" and can detect when it's running in a K8s environment.

### Spring Cloud Kubernetes
Provides integration with K8s native services (DiscoveryClient, ConfigMap/Secrets as PropertySources).

---

## Liveness and Readiness Probes
Spring Boot Actuator automatically provides endpoints for K8s probes.

### Configuration
```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
```

### Endpoints
- **Liveness**: `/actuator/health/liveness` (Is the app alive? If no, K8s restarts it).
- **Readiness**: `/actuator/health/readiness` (Is the app ready to serve traffic? If no, K8s stops sending traffic).

---

## Externalized Configuration (ConfigMaps & Secrets)
Instead of `application.properties` inside the JAR, use K8s native objects.

### Mounting ConfigMaps as Files
```yaml
spec:
  containers:
    - name: my-app
      volumeMounts:
        - name: config-volume
          mountPath: /config
  volumes:
    - name: config-volume
      configMap:
        name: my-app-config
```
Spring Boot automatically picks up files in `/config` if configured.

---

## Graceful Shutdown & Lifecycle
Crucial for zero-downtime deployments.

### Configuration
```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```
When K8s sends a `SIGTERM`, Spring Boot stops accepting new requests and waits for active ones to finish.

---

## Service Discovery & Load Balancing in K8s
In Kubernetes, you don't need **Netflix Eureka**. K8s **Services** and **CoreDNS** handle discovery.

- **Service**: Provides a stable IP and DNS name for a set of pods.
- **Load Balancing**: K8s Service (ClusterIP) provides basic round-robin load balancing.

---

## Helm Charts for Spring Boot
Helm is a package manager for Kubernetes.

### Typical Structure
```text
my-app/
  Chart.yaml          # Metadata
  values.yaml         # Configuration values
  templates/          # K8s manifests (Deployment, Service, Ingress)
    deployment.yaml
    service.yaml
```

### Example Deployment Template
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "my-app.fullname" . }}
spec:
  replicas: {{ .Values.replicaCount }}
  template:
    spec:
      containers:
        - name: {{ .Chart.Name }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
          ports:
            - containerPort: 8080
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
```
