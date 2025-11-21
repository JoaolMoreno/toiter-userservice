## **Toiter - User Service**

O **User Service** é um dos microsserviços do ecossistema **Toiter**, responsável por gerenciar usuários, autenticação e relações de follow/unfollow. Ele lida com o cadastro, atualização de perfis e permissões de acesso, além de emitir eventos para outros serviços quando ações de relacionamento ou atualizações de perfis ocorrem.

---

### **Funcionalidades Principais**

#### **1. Gerenciamento de Usuários**
- Cadastro de usuários com validações de email, senha e username.
- Atualização de perfil (bio, username, e-mail).
- Atualização de imagens (perfil e cabeçalho), com lógica para reuso de imagens já existentes.
- Exibição de dados públicos dos usuários.
- Integração com Redis para cache de mapeamentos `username -> userId` e dados públicos.

#### **2. Autenticação e Autorização**
- **Autenticação baseada em HttpOnly Cookies** (segura contra XSS):
    - JWT armazenado em cookies HttpOnly e Secure.
    - Tokens nunca expostos ao JavaScript do navegador.
    - Inclui `userId` e `username` no token.
    - Suporte a refresh token para renovação automática.
- **WebSocket Authentication**:
    - Autenticação via cookies durante o handshake HTTP.
    - Sem necessidade de enviar tokens em headers STOMP.
- **Compatibilidade com clientes não-browser**:
    - Suporte a header `Authorization: Bearer <token>` como fallback.
    - Rotas `/internal/**` utilizam chave compartilhada.
- Endpoints protegidos para garantir que cada usuário possa gerenciar apenas suas próprias informações.

#### **3. Relacionamento entre Usuários**
- Seguir outros usuários (`follow`).
- Deixar de seguir usuários (`unfollow`).
- Listagem de seguidores e seguidos.
- Emissão de eventos para integração com serviços como Feed e Notificações.

#### **4. Integração com Kafka**
- **Emissão de Eventos**:
    - `FollowCreatedEvent`: Quando um usuário segue outro.
    - `FollowDeletedEvent`: Quando um usuário deixa de seguir outro.
    - `UserUpdatedEvent`: Quando informações de um usuário são atualizadas (bio, username ou imagens).
- **Consumo de Eventos**:
    - Incrementa/Decrementa seguidores no Redis ao consumir os eventos:
        - `follow-created-topic`
        - `follow-deleted-topic`
    - Atualiza informações no Redis ao consumir:
        - `user-updated-topic`.

---

### **Autenticação com HttpOnly Cookies - Guia para Frontend**

#### **Fluxo de Login**
```javascript
// 1. Login do usuário
const response = await axios.post('/api/auth/login', {
  usernameOrEmail: 'usuario',
  password: 'senha123'
}, { withCredentials: true });

// Response: { expiresIn: 3600, message: "Login realizado com sucesso" }
// Cookies HttpOnly são definidos automaticamente: accessToken, refresh_token

// 2. Buscar dados do usuário
const user = await axios.get('/api/users/me', { withCredentials: true });
```

#### **Configuração do Axios**
```javascript
// Habilitar envio de cookies em todas as requisições
axios.defaults.withCredentials = true;

// Interceptor para renovação automática de token
axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      
      // Renovar token
      await axios.post('/api/auth/refresh', {}, { withCredentials: true });
      
      // Retry da requisição original
      return axios(error.config);
    }
    return Promise.reject(error);
  }
);
```

#### **Conexão WebSocket**
```javascript
// Conectar ao WebSocket - cookies enviados automaticamente
const socket = new SockJS('/api/chat');
const stompClient = Stomp.over(socket);

// Conectar sem header Authorization
stompClient.connect({}, (frame) => {
  console.log('Conectado:', frame);
  
  // Enviar mensagem
  stompClient.send('/app/chat/123/message', {}, 'Hello!');
});
```

#### **Logout**
```javascript
// Limpar cookies de autenticação
await axios.post('/api/auth/logout', {}, { withCredentials: true });
```

#### **Benefícios de Segurança**
- ✅ **Proteção contra XSS**: Tokens nunca acessíveis ao JavaScript
- ✅ **HttpOnly + Secure**: Cookies protegidos contra roubo
- ✅ **Renovação Automática**: Token refresh transparente
- ✅ **WebSocket Seguro**: Autenticação via cookie no handshake
- ✅ **Sem Manipulação de Tokens**: Browser gerencia cookies automaticamente

---

### **Endpoints Disponíveis**

#### **1. Autenticação**
| Método   | Endpoint            | Descrição                                             |
|----------|---------------------|-------------------------------------------------------|
| `POST`   | `/auth/register`    | Registro de um novo usuário.                          |
| `POST`   | `/auth/login`       | Login - define cookies HttpOnly (accessToken, refresh_token). |
| `POST`   | `/auth/refresh`     | Renova o accessToken usando refresh_token do cookie.  |
| `POST`   | `/auth/logout`      | Limpa os cookies de autenticação.                     |
| `GET`    | `/auth/check-session` | Verifica validade da sessão atual.                   |

#### **2. Usuários**
| Método   | Endpoint                   | Descrição                                                |
|----------|----------------------------|----------------------------------------------------------|
| `PUT`    | `/users/`                  | Atualiza username, e-mail ou bio do próprio usuário.     |
| `PUT`    | `/users/profile-image`     | Atualiza a imagem de perfil do usuário autenticado.      |
| `PUT`    | `/users/header-image`      | Atualiza a imagem de cabeçalho do usuário autenticado.   |
| `GET`    | `/users/username/{username}` | Retorna os dados públicos de um usuário por username.    |
| `GET`    | `/users/images/{id}`       | Retorna o conteúdo de uma imagem (perfil ou cabeçalho) pelo ID. |

#### **3. Relacionamentos**
| Método   | Endpoint                      | Descrição                                                |
|----------|-------------------------------|----------------------------------------------------------|
| `POST`   | `/follows/{username}/follow`  | Seguir um usuário.                                       |
| `DELETE` | `/follows/{username}/unfollow`| Deixar de seguir um usuário.                            |
| `GET`    | `/follows/{username}/followers` | Listar seguidores de um usuário.                        |
| `GET`    | `/follows/{username}/followings` | Listar usuários que o usuário está seguindo.            |

#### **4. Chat e WebSocket**
| Tipo     | Endpoint                      | Descrição                                                |
|----------|-------------------------------|----------------------------------------------------------|
| `POST`   | `/chats/start/{username}`     | Iniciar um chat com outro usuário.                       |
| `POST`   | `/chats/{chatId}/message`     | Enviar mensagem via HTTP (REST).                         |
| `GET`    | `/chats/{chatId}/messages`    | Recuperar mensagens de um chat (paginado).               |
| `GET`    | `/chats/my-chats`             | Listar chats do usuário autenticado.                     |
| `WS`     | `/chat` (SockJS)              | Endpoint WebSocket para conexão STOMP.                   |
| `STOMP`  | `/app/chat/{chatId}/message`  | Enviar mensagem em tempo real via WebSocket.            |
| `SUB`    | `/user/queue/chat`            | Subscrição para receber mensagens do usuário.           |

---

### **Consumo de Eventos Kafka**

#### **1. Eventos de Seguidores**

##### **`FollowCreatedEvent`**
- Incrementa o contador de seguidores (`followersCount`) de um usuário no Redis.
- O incremento é realizado apenas se o usuário já estiver presente no cache Redis.

##### **`FollowDeletedEvent`**
- Decrementa o contador de seguidores (`followersCount`) de um usuário no Redis.
- O decremento é realizado apenas se o usuário já estiver presente no cache Redis e nunca vai abaixo de 0.

#### **2. Evento de Atualização de Usuário**

##### **`UserUpdatedEvent`**
- Atualiza as informações no Redis para o `userId` especificado no evento.
- Campos atualizados:
    - `username`
    - `bio`
    - `profileImageId`
    - `headerImageId`
    - Outros campos públicos relevantes.
- **Regras de atualização:**
    - Se o `username` foi alterado, o mapeamento `username -> userId` também é atualizado.
    - Os dados no Redis são sobrescritos com base no evento.

---

### **Arquitetura e Tecnologias**

#### **1. Banco de Dados**
- **PostgreSQL**:
    - Tabelas:
        - `users`: Gerencia informações dos usuários.
        - `images`: Gerencia imagens (perfil e cabeçalho).
        - `follows`: Relacionamento de seguidores e seguidos.
    - Restrições:
        - `UNIQUE` em `username` e `email`.

#### **2. Mensageria**
- **Apache Kafka**:
    - Tópicos:
        - `follow-created-topic`
        - `follow-deleted-topic`
        - `user-updated-topic`

#### **3. Cache**
- **Redis**:
    - Mapeamento `username -> userId`.
    - Dados públicos do usuário (`userId -> UserPublicData`).
    - Contagem de seguidores (`followersCount`) atualizada em tempo real.

#### **4. Segurança**
- **Spring Security com JWT em HttpOnly Cookies**:
    - Tokens armazenados em cookies HttpOnly e Secure.
    - **accessToken**: Cookie com path `/` para todas as APIs.
    - **refresh_token**: Cookie com path `/auth/refresh` para renovação.
    - Proteção contra XSS (tokens não acessíveis via JavaScript).
    - Proteção contra CSRF via verificação de origem em WebSockets.
    - CORS configurado com `allowCredentials: true` para origens permitidas.
- **Autenticação de WebSocket**:
    - Endpoint STOMP em `/chat` (via `/api/chat` com context-path).
    - Autenticação durante handshake HTTP usando cookies.
    - Sem necessidade de tokens em headers STOMP.
- **Controle de Acesso**:
    - Tokens contêm `userId` e `username`.
    - Endpoints protegidos baseados no usuário autenticado.
    - Rotas `/internal/**` protegidas com chave compartilhada.
- **Rate Limiting**:
    - Rate limiting por usuário implementado com Redis.
    - Diferentes limites baseados no tipo de requisição:
        - **GET**: 100 requisições por minuto
        - **POST/PUT/DELETE**: 30 requisições por minuto
        - **Login**: 5 tentativas por minuto
    - Headers de resposta incluem informações de limite:
        - `X-RateLimit-Limit`: Limite total de requisições
        - `X-RateLimit-Remaining`: Requisições restantes
        - `X-RateLimit-Reset`: Timestamp quando o limite é resetado
    - Retorna HTTP 429 (Too Many Requests) quando o limite é excedido.

#### **5. Framework**
- **Spring Boot**:
    - Camadas:
        - **Controller**: Endpoints REST.
        - **Service**: Regras de negócio.
        - **Repository**: Acesso ao banco de dados via JPA.
        - **Producer**: Emissão de eventos Kafka.
        - **Consumer**: Consumo de eventos Kafka para atualizar Redis.

---

### **Como Executar**

1. Clone o repositório:
   ```bash
   git clone https://github.com/JoaolMoreno/toiter-userservice.git
   cd toiter-user-service
   ```

2. Configure o arquivo `application.properties` com:
    - Detalhes do banco de dados PostgreSQL.
    - Chave secreta para JWT.
    - Configuração do Kafka e Redis.
    - Configuração de rate limiting (opcional):
        - `rate-limit.get.requests`: Limite de requisições GET por minuto (padrão: 100)
        - `rate-limit.get.window-seconds`: Janela de tempo para GET em segundos (padrão: 60)
        - `rate-limit.other.requests`: Limite de outras requisições por minuto (padrão: 30)
        - `rate-limit.other.window-seconds`: Janela de tempo para outras requisições em segundos (padrão: 60)
        - `rate-limit.login.requests`: Limite de tentativas de login por minuto (padrão: 5)
        - `rate-limit.login.window-seconds`: Janela de tempo para login em segundos (padrão: 60)

3. Suba os serviços com Docker Compose:
   ```bash
   docker-compose up
   ```

4. Acesse a API localmente em:
   ```bash
   http://localhost:9990
   ```

---

### **Estrutura do Projeto**

```
toiter-user-service/
├── src/
│   ├── main/
│   │   ├── java/com/toiter/
│   │   │   ├── userservice/
│   │   │   │   ├── controller/       # Controladores REST
│   │   │   │   ├── service/          # Lógica de negócio
│   │   │   │   ├── repository/       # Acesso ao banco de dados
│   │   │   │   ├── model/            # DTOs e eventos Kafka
│   │   │   │   ├── entity/           # Entidades JPA
│   │   │   │   ├── config/           # Configurações do Spring e Kafka
│   │   │   │   ├── producer/         # Emissão de eventos Kafka
│   │   │   │   ├── consumer/         # Consumo de eventos Kafka
│   │   │   │   └── exception/        # Tratamento de exceções globais
│   │   └── resources/
│   │       ├── application.properties    # Configurações da aplicação
│   │       └── application-dev.properties # Configurações para ambiente de desenvolvimento
└── docker-compose.yml                # Configuração para subir serviços
```

---

### **Melhorias Futuras**
1. **Armazenamento de Imagens**:
    - Migrar para um serviço externo (e.g., AWS S3) para escalabilidade.

2. **Monitoramento**:
    - Integração com ferramentas como Prometheus e Grafana para monitoramento em tempo real.

3. **Sincronização do Cache**:
    - Melhorar a sincronização entre o Redis e o banco de dados em cenários de inconsistência.

---

### **Licença**
Este projeto é livre para uso sob a licença [MIT](LICENSE).
