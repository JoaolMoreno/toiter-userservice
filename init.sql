-- Este script cria o schema 'usr' e as tabelas 'images', 'users' e 'followers' no banco de dados.
-- O schema 'usr' é criado para separar as tabelas do banco de dados de outras tabelas que podem existir no banco de dados.
-- Esse script deve ser executado ao configurar o banco de dados pela primeira vez.
-- Criar o schema 'usr'
CREATE SCHEMA usr;

-- Alterar a propriedade do schema para o usuário 'usr'
ALTER SCHEMA usr OWNER TO usr;

-- Create the Images table
CREATE TABLE usr.images (
                            id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            image BYTEA,
                            url VARCHAR(2083),
                            creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index for quick lookup on id
CREATE INDEX idx_images_id ON usr.images (id);

-- Create the Users table
CREATE TABLE usr.users (
                           id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           username VARCHAR(255) NOT NULL,
                           display_name VARCHAR(30) NOT NULL,
                           email VARCHAR(255) UNIQUE NOT NULL,
                           password VARCHAR(255) NOT NULL,
                           bio TEXT,
                           profile_image_id BIGINT,
                           header_image_id BIGINT,
                           creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           FOREIGN KEY (profile_image_id) REFERENCES usr.images (id) ON DELETE SET NULL,
                           FOREIGN KEY (header_image_id) REFERENCES usr.images (id) ON DELETE SET NULL
);

-- Add constraints for email and username uniqueness
ALTER TABLE usr.users ADD CONSTRAINT users_email_unique UNIQUE (email);
ALTER TABLE usr.users ADD CONSTRAINT users_username_unique UNIQUE (username);

-- Index for email lookup
CREATE INDEX idx_users_email ON usr.users (email);

-- Index for username lookup
CREATE INDEX idx_users_username ON usr.users (username);

-- Index for display_name lookup
CREATE INDEX idx_users_display_name ON usr.users (display_name);

-- Index for id lookup
CREATE INDEX idx_users_id ON usr.users (id);

-- Create the Followers table
CREATE TABLE usr.followers (
                               id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               follower_id BIGINT NOT NULL,
                               follow_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (user_id) REFERENCES usr.users (id) ON DELETE CASCADE,
                               FOREIGN KEY (follower_id) REFERENCES usr.users (id) ON DELETE CASCADE,
                               UNIQUE (user_id, follower_id)
);

-- Create indexes to optimize queries on the Followers table
CREATE INDEX idx_followers_user_id ON usr.followers (user_id);
CREATE INDEX idx_followers_follower_id ON usr.followers (follower_id);

DO $$
    BEGIN
        EXECUTE (
            SELECT string_agg('ALTER TABLE ' || table_schema || '.' || table_name || ' OWNER TO usr;', ' ')
            FROM information_schema.tables
            WHERE table_schema = 'usr'
        );
    END $$;

CREATE VIEW vw_users AS
SELECT
    id,
    username,
    display_name,
    email,
    bio,
    profile_image_id,
    header_image_id,
    creation_date
FROM usr.users;

GRANT SELECT ON vw_users TO usr;
GRANT SELECT ON vw_users TO pst;

-- Tabela de Conversas
CREATE TABLE usr.chats (
                           id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           user_id1 BIGINT NOT NULL,
                           user_id2 BIGINT NOT NULL,
                           creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           FOREIGN KEY (user_id1) REFERENCES usr.users (id) ON DELETE CASCADE,
                           FOREIGN KEY (user_id2) REFERENCES usr.users (id) ON DELETE CASCADE,
                           CONSTRAINT unique_chat_users UNIQUE (user_id1, user_id2),
                           CONSTRAINT check_user_order CHECK (user_id1 < user_id2)
);

-- Índices para user_id1 e user_id2
CREATE INDEX idx_chats_user_id1 ON usr.chats (user_id1);
CREATE INDEX idx_chats_user_id2 ON usr.chats (user_id2);

-- Tabela de Mensagens
CREATE TABLE usr.messages (
                              id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              chat_id BIGINT NOT NULL,
                              sender_id BIGINT NOT NULL,
                              content TEXT NOT NULL,
                              sent_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (chat_id) REFERENCES usr.chats (id) ON DELETE CASCADE,
                              FOREIGN KEY (sender_id) REFERENCES usr.users (id) ON DELETE CASCADE
);

-- Índice composto para chat_id e sent_date
CREATE INDEX idx_messages_chat_id_sent_date ON usr.messages (chat_id, sent_date DESC);