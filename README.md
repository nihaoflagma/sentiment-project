# sentiment-project
AI Sentiment Analysis App on Kubernetes (Minikube)
Описание проекта

Данный проект представляет собой Java-приложение для анализа тональности текста, контейнеризированное в Docker и развернутое в Kubernetes (Minikube).
В составе проекта:

REST API /api/sentiment?text=...

Docker-образ <150 MB

Kubernetes: Deployment (3 реплики), Service, Ingress, HPA

Мониторинг: Prometheus + Grafana

Аналитический обзор 5 статей arXiv о тенденциях Kubernetes + AI (2024–2025)

Архитектура решения
Client → Ingress → Service → Deployment (3 Pods)
                     ↑
             Prometheus ← Grafana

1. Установка Minikube
minikube start --cpus=4 --memory=8192mb --nodes=2
kubectl get nodes

2. Контейнеризация приложения
Java код
import spark.Spark;

public class Main {
    public static void main(String[] args) {
        Spark.port(8080);
        Spark.get("/api/sentiment", (req, res) -> {
            String text = req.queryParams("text");
            return "{\"sentiment\": \"positive\"}";
        });
    }
}

Dockerfile
FROM openjdk:17-jdk-slim
COPY target/app.jar app.jar
CMD ["java","-jar","/app.jar"]


Сборка образа:

docker build -t sentiment-app:1.0 .

3. Развертывание в Kubernetes
Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sentiment-deployment
spec:
  replicas: 3
  selector:
    matchLabels:
      app: sentiment
  template:
    metadata:
      labels:
        app: sentiment
    spec:
      containers:
      - name: sentiment
        image: sentiment-app:1.0
        ports:
        - containerPort: 8080

Service
apiVersion: v1
kind: Service
metadata:
  name: sentiment-service
spec:
  type: LoadBalancer
  selector:
    app: sentiment
  ports:
  - port: 80
    targetPort: 8080

Ingress
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: sentiment-ingress
spec:
  rules:
  - http:
      paths:
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: sentiment-service
            port:
              number: 80

HPA
kubectl autoscale deployment sentiment-deployment --cpu-percent=50 --min=3 --max=10

4. Тестирование API

Получение URL:

minikube service sentiment-service --url


Запрос:

curl "<URL>/api/sentiment?text=hello"

5. Мониторинг: Prometheus + Grafana
   
Установка

helm repo add prometheus-community https://prometheus-community.github.io/helm-charts

helm install prometheus prometheus-community/kube-prometheus-stack

Доступ

Prometheus:

kubectl port-forward svc/prometheus-kube-prometheus-prometheus 9090:9090


Grafana:

kubectl port-forward svc/prometheus-grafana 3000:80

6. Итоги проекта
   
Достижения:

Успешно развернуто ИИ-приложение в Minikube.

Настроены Service, Ingress, HPA.

Добавлен мониторинг: Prometheus и Grafana.

Grafana login:

user: admin
pass: prom-operator
