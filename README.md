## **Toiter - User Service**

O **User Service** é um dos microsserviços do ecossistema **Toiter**, responsável por gerenciar usuários, autenticação e relações de follow/unfollow. Ele lida com o cadastro, atualização de perfis e permissões de acesso, além de emitir eventos para outros serviços quando ações de relacionamento ocorrem.

---

### **Funcionalidades Principais**

#### **1. Gerenciamento de Usuários**
- Cadastro de usuários com validações de email, senha e username.
- Atualização de perfil (bio, username, e-mail).
- Atualização de imagens (perfil e cabeçalho).
- Exibição de dados públicos dos usuários.

#### **2. Autenticação e Autorização**
- Login com JWT:
    - Inclui `userId` e `username` no token.
    - Controle de permissões baseado no usuário autenticado.
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
- Outros serviços (e.g., Feed, Notificações) consomem esses eventos para manter a consistência entre os dados.

---

### **Endpoints Disponíveis**

#### **1. Autenticação**
| Método   | Endpoint         | Descrição                    |
|----------|------------------|------------------------------|
| `POST`   | `/auth/register` | Registro de um novo usuário. |
| `POST`   | `/auth/login`    | Login e geração de JWT.      |

#### **2. Usuários**
| Método   | Endpoint                   | Descrição                                                |
|----------|----------------------------|----------------------------------------------------------|
| `PUT`    | `/users/`                  | Atualiza username, e-mail ou bio do próprio usuário.     |
| `PUT`    | `/users/profile-image`     | Atualiza a imagem de perfil do usuário autenticado.      |
| `PUT`    | `/users/header-image`      | Atualiza a imagem de cabeçalho do usuário autenticado.   |
| `GET`    | `/users/username/{username}` | Retorna os dados públicos de um usuário por username.    |
| `GET`    | `/users/images/{id}`       | Retorna uma imagem (perfil ou cabeçalho) pelo ID.        |

#### **3. Relacionamentos**
| Método   | Endpoint                      | Descrição                                                |
|----------|-------------------------------|----------------------------------------------------------|
| `POST`   | `/follows/{username}/follow`  | Seguir um usuário.                                       |
| `DELETE` | `/follows/{username}/unfollow`| Deixar de seguir um usuário.                            |
| `GET`    | `/follows/{username}/followers` | Listar seguidores de um usuário.                        |
| `GET`    | `/follows/{username}/followings` | Listar usuários que o usuário está seguindo.            |

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

#### **3. Segurança**
- **Spring Security com JWT**:
    - Tokens contêm `userId` e `username`.
    - Controle de acesso aos endpoints baseado no usuário autenticado.

#### **4. Framework**
- **Spring Boot**:
    - Camadas:
        - **Controller**: Endpoints REST.
        - **Service**: Regras de negócio.
        - **Repository**: Acesso ao banco de dados via JPA.
        - **Config**: Configurações de segurança, Kafka, entre outros.

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
    - Configuração do Kafka.

3. Suba os serviços com Docker Compose:
   ```bash
   docker-compose up
   ```

4. Acesse a API localmente em:
   ```
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
│   │   │   │   ├── model/            # DTOs e modelos de dados
│   │   │   │   ├── entity/           # Entidades JPA
│   │   │   │   ├── config/           # Configurações do Spring e Kafka
│   │   │   │   ├── producer/         # Emissão de eventos Kafka
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

---

### **Licença**
Este projeto é livre para uso sob a licença [MIT](LICENSE).