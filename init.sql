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
    email,
    bio,
    profile_image_id,
    header_image_id,
    creation_date
FROM usr.users;

GRANT SELECT ON vw_users TO usr;
GRANT SELECT ON vw_users TO pst;