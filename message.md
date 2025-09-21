# Guide de Test du Microservice Message

Ce guide vous permettra de tester complètement le microservice `message_service` depuis le démarrage jusqu'aux fonctionnalités avancées.

## Étape 1 : Préparation et Démarrage

### 1.1 Placement des fichiers
```bash
# Vérifiez que ces fichiers sont bien placés :
./init/03-create-messages-table.sql
./Dockerfile.message_service
./message_service/ (dossier complet avec src/)
```

### 1.2 Démarrage des services
```bash
# Démarrage complet de l'écosystème
docker-compose up -d

# Vérification que les services sont démarrés
docker-compose ps

# Logs du message service spécifiquement
docker-compose logs -f message-service
```

### 1.3 Vérification de la base de données
```bash
# Connectez-vous à pgAdmin (http://localhost:5051)
# Email: admin@hypersend.com / Password: admin123
# Vérifiez que la table 'messages' existe avec les index
```

## Étape 2 : Tests de Base - Authentication

### 2.1 Obtenir un token JWT
```bash
# D'abord, connectez-vous pour obtenir un token
curl -X POST http://localhost:8082/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "admin",
    "password": "password123"
  }'
```

**Réponse attendue :**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "username": "admin",
    "email": "admin@hypersend.com"
  }
}
```

### 2.2 Test de santé du service
```bash
# Vérifiez que le service répond
curl -X GET http://localhost:8083/api/v1/messages/health
```

**Réponse attendue :**
```json
{
  "success": true,
  "data": "Message service is running"
}
```

## Étape 3 : Tests des Fonctionnalités de Base

### 3.1 Envoi d'un premier message
```bash
# Remplacez YOUR_JWT_TOKEN par le token obtenu à l'étape 2.1
curl -X POST http://localhost:8083/api/v1/messages/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "receiverId": 2,
    "content": "Bonjour! Premier message de test."
  }'
```

**Réponse attendue :**
```json
{
  "success": true,
  "message": "Message sent successfully",
  "data": {
    "id": 1,
    "senderId": 1,
    "senderUsername": "admin",
    "receiverId": 2,
    "receiverUsername": "john_doe",
    "content": "Bonjour! Premier message de test.",
    "createdAt": "2025-01-01 12:00:00",
    "updatedAt": "2025-01-01 12:00:00"
  }
}
```

### 3.2 Test d'erreurs de validation
```bash
# Message vide (doit échouer)
curl -X POST http://localhost:8083/api/v1/messages/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "receiverId": 2,
    "content": ""
  }'

# Utilisateur inexistant (doit échouer)
curl -X POST http://localhost:8083/api/v1/messages/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "receiverId": 9999,
    "content": "Message vers utilisateur inexistant"
  }'
```

## Étape 4 : Tests des Conversations

### 4.1 Créer plusieurs messages
```bash
# Message 2
curl -X POST http://localhost:8083/api/v1/messages/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "receiverId": 2,
    "content": "Deuxième message pour créer une conversation."
  }'

# Connectez-vous en tant que john_doe pour répondre
curl -X POST http://localhost:8082/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "john_doe",
    "password": "password123"
  }'

# Réponse de john_doe (utilisez son token)
curl -X POST http://localhost:8083/api/v1/messages/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer JOHN_DOE_JWT_TOKEN" \
  -d '{
    "receiverId": 1,
    "content": "Salut admin! Merci pour ton message."
  }'
```

### 4.2 Récupérer une conversation complète
```bash
# Conversation entre admin (ID:1) et john_doe (ID:2)
curl -X GET http://localhost:8083/api/v1/messages/conversation/2 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Réponse attendue :**
```json
{
  "success": true,
  "message": "Conversation retrieved successfully",
  "data": {
    "otherUserId": 2,
    "otherUsername": "john_doe",
    "lastMessage": "Salut admin! Merci pour ton message.",
    "lastMessageTime": "2025-01-01 12:05:00",
    "totalMessages": 3,
    "messages": [
      {
        "id": 1,
        "senderId": 1,
        "senderUsername": "admin",
        "receiverId": 2,
        "receiverUsername": "john_doe",
        "content": "Bonjour! Premier message de test.",
        "createdAt": "2025-01-01 12:00:00",
        "updatedAt": "2025-01-01 12:00:00"
      }
      // ... autres messages
    ]
  }
}
```

### 4.3 Liste de toutes les conversations
```bash
curl -X GET http://localhost:8083/api/v1/messages/conversations \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Étape 5 : Tests Avancés

### 5.1 Pagination
```bash
# Conversation avec pagination
curl -X GET "http://localhost:8083/api/v1/messages/conversation/2/paginated?page=0&size=2" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 5.2 Historique des messages
```bash
curl -X GET "http://localhost:8083/api/v1/messages/history?limit=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 5.3 Statistiques
```bash
curl -X GET http://localhost:8083/api/v1/messages/stats \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 5.4 Résumé de conversation
```bash
curl -X GET http://localhost:8083/api/v1/messages/conversation/2/summary \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Étape 6 : Tests de Sécurité

### 6.1 Accès sans token (doit échouer)
```bash
curl -X POST http://localhost:8083/api/v1/messages/send \
  -H "Content-Type: application/json" \
  -d '{
    "receiverId": 2,
    "content": "Message sans authentification"
  }'
```

### 6.2 Token invalide (doit échouer)
```bash
curl -X GET http://localhost:8083/api/v1/messages/conversations \
  -H "Authorization: Bearer invalid_token"
```

## Étape 7 : Tests de Performance et Charge

### 7.1 Script de test de charge
```bash
# Créez un script test_load.sh
#!/bin/bash
TOKEN="YOUR_JWT_TOKEN"

for i in {1..20}; do
  curl -X POST http://localhost:8083/api/v1/messages/send \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{
      \"receiverId\": 2,
      \"content\": \"Message de test numéro $i\"
    }" &
done
wait

echo "20 messages envoyés en parallèle"
```

### 7.2 Vérification en base de données
```sql
-- Connectez-vous à PostgreSQL via pgAdmin
-- Exécutez ces requêtes pour vérifier les données

SELECT COUNT(*) FROM messages;

SELECT 
    sender_id, 
    receiver_id, 
    COUNT(*) as message_count,
    MAX(created_at) as last_message
FROM messages 
GROUP BY sender_id, receiver_id;

-- Testez la vue de statistiques
SELECT * FROM message_stats;

-- Testez la fonction helper
SELECT * FROM get_conversation_partners(1);
```

## Étape 8 : Monitoring et Logs

### 8.1 Vérification des logs
```bash
# Logs en temps réel
docker-compose logs -f message-service

# Logs des erreurs uniquement
docker-compose logs message-service | grep ERROR

# Logs des requêtes SQL
docker-compose logs message-service | grep "Hibernate:"
```

### 8.2 Métriques de santé
```bash
# Endpoint actuator
curl http://localhost:8083/actuator/health

# Métriques (si activées)
curl http://localhost:8083/actuator/metrics
```

## Scénarios de Test Complets

### Scénario 1 : Conversation Complète
1. Admin se connecte et envoie message à john_doe
2. john_doe se connecte et répond
3. Échange de plusieurs messages
4. Vérification de la conversation complète
5. Test de la pagination si +20 messages

### Scénario 2 : Multiples Conversations
1. Admin envoie messages à john_doe, jane_smith, test_user
2. Vérification de la liste des conversations
3. Test des résumés de conversation
4. Vérification des statistiques

### Scénario 3 : Gestion d'Erreurs
1. Tests avec utilisateurs inexistants
2. Tests avec contenu invalide
3. Tests avec tokens expirés
4. Tests avec paramètres incorrects

Ce guide couvre tous les aspects du microservice. Commencez par les étapes 1-3 pour vérifier le fonctionnement de base, puis progressez vers les tests avancés selon vos besoins.