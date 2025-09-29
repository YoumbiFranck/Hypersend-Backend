Points d'accès

Load Balancer : http://localhost (port 80)
HAProxy Stats : http://localhost:8404/stats (admin/hypersend2024)
Eureka Dashboard : http://localhost:8761/
PostgreSQL Master : localhost:5432
PostgreSQL Slave : localhost:5433
pgAdmin : http://localhost:5051

---

Voici les requêtes simplifiées pour Postman :

## 1. AUTHENTICATION

### Register User
```
POST http://localhost/api/v1/users/register
Content-Type: application/json

{
  "userName": "testuser",
  "email": "test@example.com",
  "password": "Test123456"
}
```

### Login
```
POST http://localhost/api/v1/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "admin",
  "password": "password123"
}
```

### Refresh Token
```
POST http://localhost/api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "YOUR_REFRESH_TOKEN"
}
```

### Get Current User
```
GET http://localhost/api/v1/auth/me
Authorization: Bearer YOUR_ACCESS_TOKEN
```

### Logout
```
POST http://localhost/api/v1/auth/logout
Authorization: Bearer YOUR_ACCESS_TOKEN
```

## 2. USERS

### Get User Profile
```
GET http://localhost/api/v1/users/profile
Authorization: Bearer YOUR_ACCESS_TOKEN
```

### Get User By ID
```
GET http://localhost/api/v1/users/2
Authorization: Bearer YOUR_ACCESS_TOKEN
```

### Search Users
```
GET http://localhost/api/v1/users/search?query=test&limit=10
Authorization: Bearer YOUR_ACCESS_TOKEN
```

## 3. MESSAGES

### Send Message
```
POST http://localhost/api/v1/messages/send
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json

{
  "receiverId": 2,
  "content": "Hello, this is a test message!"
}
```

### Get Conversation
```
GET http://localhost/api/v1/messages/conversation/2
Authorization: Bearer YOUR_ACCESS_TOKEN
```

### Get Conversation (Paginated)
```
GET http://localhost/api/v1/messages/conversation/2/paginated?page=0&size=20
Authorization: Bearer YOUR_ACCESS_TOKEN
```

### Get All Conversations
```
GET http://localhost/api/v1/messages/conversations
Authorization: Bearer YOUR_ACCESS_TOKEN
```

### Get Message History
```
GET http://localhost/api/v1/messages/history?limit=50
Authorization: Bearer YOUR_ACCESS_TOKEN
```

### Get Conversation Summary
```
GET http://localhost/api/v1/messages/conversation/2/summary
Authorization: Bearer YOUR_ACCESS_TOKEN
```

### Get Message Stats
```
GET http://localhost/api/v1/messages/stats
Authorization: Bearer YOUR_ACCESS_TOKEN
```

## 4. MONITORING

### HAProxy Stats
```
GET http://localhost:8404/stats
Authorization: Basic admin:hypersend2024
```

### Eureka Dashboard
```
GET http://localhost:8761/
```

### Eureka Apps
```
GET http://localhost:8761/eureka/apps
```

## Ordre de test recommandé :

1. **Register User** → crée un nouvel utilisateur
2. **Login** → récupère le `accessToken` (copiez-le !)
3. **Get Current User** → teste l'authentification
4. **Send Message** → envoie un message à l'utilisateur ID 2
5. **Get Conversations** → voir toutes les conversations
6. **Get Message Stats** → statistiques

**Note :** Remplacez `YOUR_ACCESS_TOKEN` par le token obtenu lors du login.