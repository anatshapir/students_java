# JavaEdu Platform - Setup Guide

This guide covers how to set up and deploy the JavaEdu platform components.

## Prerequisites

- Java 17 JDK
- Node.js 18+ and npm
- PostgreSQL 14+
- Eclipse IDE (2022+ for plugin development)
- Maven 3.8+

## Backend Setup (javaedu-server)

### 1. Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE javaedu;
CREATE USER javaedu WITH PASSWORD 'javaedu';
GRANT ALL PRIVILEGES ON DATABASE javaedu TO javaedu;
```

### 2. Configuration

Create environment variables or edit `application.yml`:

```bash
# Required
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=javaedu
export DB_USERNAME=javaedu
export DB_PASSWORD=javaedu
export JWT_SECRET=your-256-bit-secret-key-minimum-32-characters-long

# Optional - for integrations
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
export GITHUB_CLIENT_ID=your-github-client-id
export GITHUB_CLIENT_SECRET=your-github-client-secret
export AI_API_KEY=your-anthropic-api-key
```

### 3. Build and Run

```bash
cd javaedu-server
mvn clean package
java -jar target/javaedu-server-1.0.0-SNAPSHOT.jar
```

The server will start at `http://localhost:8080`.

### 4. Verify

Test the health endpoint:

```bash
curl http://localhost:8080/api/health
```

## Teacher Dashboard Setup (javaedu-teacher-dashboard)

### 1. Install Dependencies

```bash
cd javaedu-teacher-dashboard
npm install
```

### 2. Configure API URL

The dashboard proxies API requests to `http://localhost:8080` by default. Edit `vite.config.ts` to change this.

### 3. Run Development Server

```bash
npm run dev
```

The dashboard will be available at `http://localhost:3000`.

### 4. Build for Production

```bash
npm run build
```

Static files will be in the `dist` folder.

## Eclipse Plugin Setup (javaedu-eclipse-plugin)

### 1. Import into Eclipse

1. Open Eclipse IDE for Plugin Development
2. File > Import > Existing Projects into Workspace
3. Select the `javaedu-eclipse-plugin` folder
4. Click Finish

### 2. Configure Server URL

Edit the preferences at Window > Preferences > JavaEdu to set the server URL.

### 3. Run for Testing

1. Right-click the project > Run As > Eclipse Application
2. A new Eclipse instance will open with the plugin installed

### 4. Export Plugin

1. Right-click the project > Export > Plug-in Development > Deployable plug-ins
2. Choose an output directory
3. Distribute the resulting JAR file

## Oracle Cloud Deployment

### 1. Create VM Instance

1. Sign up at cloud.oracle.com
2. Create a VM.Standard.A1.Flex instance (ARM)
3. Configure: 2 OCPUs, 4GB RAM, Ubuntu 22.04
4. Add SSH key for access

### 2. Configure Networking

Open ports in the security list:
- 22 (SSH)
- 80 (HTTP)
- 443 (HTTPS)

### 3. Install Dependencies on Server

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk postgresql nginx certbot python3-certbot-nginx
```

### 4. Setup PostgreSQL

```bash
sudo -u postgres psql
CREATE DATABASE javaedu;
CREATE USER javaedu WITH PASSWORD 'secure-password';
GRANT ALL PRIVILEGES ON DATABASE javaedu TO javaedu;
\q
```

### 5. Deploy Backend

```bash
# Create app directory
sudo mkdir -p /opt/javaedu
sudo chown $USER:$USER /opt/javaedu

# Upload JAR file
scp target/javaedu-server-1.0.0-SNAPSHOT.jar user@server:/opt/javaedu/

# Create systemd service
sudo nano /etc/systemd/system/javaedu.service
```

Add to `/etc/systemd/system/javaedu.service`:

```ini
[Unit]
Description=JavaEdu Server
After=network.target postgresql.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/javaedu
ExecStart=/usr/bin/java -jar javaedu-server-1.0.0-SNAPSHOT.jar
Restart=always
Environment=DB_HOST=localhost
Environment=DB_NAME=javaedu
Environment=DB_USERNAME=javaedu
Environment=DB_PASSWORD=secure-password
Environment=JWT_SECRET=your-production-secret

[Install]
WantedBy=multi-user.target
```

Start the service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable javaedu
sudo systemctl start javaedu
```

### 6. Configure Nginx

```bash
sudo nano /etc/nginx/sites-available/javaedu
```

Add:

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        root /var/www/javaedu;
        try_files $uri $uri/ /index.html;
    }
}
```

Enable and restart:

```bash
sudo ln -s /etc/nginx/sites-available/javaedu /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### 7. Setup SSL (Optional but Recommended)

```bash
sudo certbot --nginx -d your-domain.com
```

## Troubleshooting

### Backend won't start

- Check PostgreSQL is running: `sudo systemctl status postgresql`
- Verify database connection settings
- Check logs: `journalctl -u javaedu -f`

### Database migrations fail

- Ensure the database user has CREATE permission
- Check `src/main/resources/db/migration` for SQL syntax errors

### Plugin can't connect

- Verify the server URL in preferences
- Check if the server is running
- Look for CORS errors in browser console

## Initial Data

Create an admin/teacher account:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Teacher",
    "email": "teacher@example.com",
    "password": "password123",
    "role": "TEACHER"
  }'
```
