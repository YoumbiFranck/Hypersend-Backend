## Étapes pour accéder à votre base de données

### 1. **Connexion à pgAdmin**
- URL : http://localhost:5051/
- Email : `admin@hypersend.com` (selon votre .env)
- Mot de passe : `admin123` (selon votre .env)

### 2. **Ajouter une connexion serveur**

Une fois connecté à pgAdmin :

1. **Clic droit** sur "Servers" (dans le panneau de gauche)
2. **Register** → **Server...**

### 3. **Configuration de la connexion**

**Onglet "General"** :
- Name : `Hypersend Database` (ou n'importe quel nom)

**Onglet "Connection"** :
- Host name/address : `hps_postgres` (nom du service Docker)
- Port : `5432` (port interne, pas 5433)
- Maintenance database : `hypersend`
- Username : `hypersend_user`
- Password : `hypersend_password`

### 4. **Ce que vous verrez**

Après connexion, vous devriez voir :
```
Servers
└── Hypersend Database
    └── Databases
        └── hypersend
            └── Schemas
                └── public
                    └── Tables
                        └── app_user  ← Votre table créée
```

### 5. **Explorer vos données**

Pour voir les données de test :
1. **Développez** : Servers → Hypersend Database → Databases → hypersend → Schemas → public → Tables
2. **Clic droit** sur `app_user` → **View/Edit Data** → **All Rows**

Vous devriez voir les 4 utilisateurs de test que nous avons créés :

| id | user_name | email | password (hashé) |
|----|-----------|-------|------------------|
| 1 | admin | admin@hypersend.com | $2a$10$92IX... |
| 2 | john_doe | john.doe@hypersend.com | $2a$10$92IX... |
| 3 | jane_smith | jane.smith@hypersend.com | $2a$10$92IX... |
| 4 | test_user | test@hypersend.com | $2a$10$92IX... |

## Résolution de problèmes

### Si pgAdmin ne se charge pas :
```bash
# Vérifier que pgAdmin tourne
docker-compose ps hps_pgadmin

# Voir les logs de pgAdmin
docker-compose logs hps_pgadmin

# Redémarrer pgAdmin si nécessaire
docker-compose restart hps_pgadmin
```

### Si la base de données n'apparaît pas :
```bash
# Vérifier que PostgreSQL tourne et que la base existe
docker exec -it hps_postgres_db psql -U hypersend_user -l

# Doit montrer 'hypersend' dans la liste des bases
```

### Si les scripts d'initialisation n'ont pas tourné :
```bash
# Recréer complètement pour relancer l'initialisation
docker-compose down -v
docker-compose up -d hps_postgres
docker-compose logs hps_postgres_db
```

