
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- --------------------------------------------------
-- 0) ENUMs (sólo se crean si no existen)
-- --------------------------------------------------
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'roles_choices') THEN
CREATE TYPE roles_choices AS ENUM ('GUEST','VIEWER','CREATOR','OWNER','ADMIN');
END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'geojson_type_choices') THEN
CREATE TYPE geojson_type_choices AS ENUM ('Feature','FeatureCollection');
END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'geojson_attr_type_choices') THEN
CREATE TYPE geojson_attr_type_choices AS ENUM ('int','float','str','bool', 'NoneType');
END IF;
END$$;


CREATE TABLE IF NOT EXISTS auth_user (
     id SERIAL PRIMARY KEY,
     username VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(128) NOT NULL,
    email VARCHAR(254) UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_staff BOOLEAN NOT NULL DEFAULT FALSE,
    is_superuser BOOLEAN NOT NULL DEFAULT FALSE,
    date_joined TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login TIMESTAMPTZ
    );

-- --------------------------------------------------
-- 1) Tabla base de historial (Digital_Resource)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS digital_resource (
                                                id SERIAL PRIMARY KEY,
                                                creator_id INTEGER REFERENCES auth_user(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    editor_id INTEGER REFERENCES auth_user(id) ON DELETE SET NULL,
    edited_at TIMESTAMPTZ,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
    );


-- --------------------------------------------------
-- 2) Equipos (hereda de Digital_Resource)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS team (
                                    digitalresource_ptr_id INTEGER PRIMARY KEY
                                    REFERENCES digital_resource(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL
    );


-- --------------------------------------------------
-- 3) Roles (hereda de Digital_Resource)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS role (
                                    digitalresource_ptr_id INTEGER PRIMARY KEY
                                    REFERENCES digital_resource(id) ON DELETE CASCADE,
    role_name roles_choices NOT NULL
    );


-- --------------------------------------------------
-- 4) Membership (hereda de Digital_Resource)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS membership (
                                          digitalresource_ptr_id INTEGER PRIMARY KEY
                                          REFERENCES digital_resource(id) ON DELETE CASCADE,
    member_id INTEGER NOT NULL
    REFERENCES auth_user(id) ON DELETE CASCADE,
    user_team_id INTEGER
    REFERENCES team(digitalresource_ptr_id) ON DELETE SET NULL,
    user_role_id INTEGER NOT NULL
    REFERENCES role(digitalresource_ptr_id) ON DELETE SET DEFAULT
    );


-- --------------------------------------------------
-- 5) Proyectos (hereda de Digital_Resource)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS project (
                                       digitalresource_ptr_id INTEGER PRIMARY KEY
                                       REFERENCES digital_resource(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    finished BOOLEAN NOT NULL DEFAULT FALSE
    );


-- --------------------------------------------------
-- 6) Asignaciones de equipos a proyectos (hereda)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS assignations (
                                            digitalresource_ptr_id INTEGER PRIMARY KEY
                                            REFERENCES digital_resource(id) ON DELETE CASCADE,
    assignated_project_id INTEGER
    REFERENCES project(digitalresource_ptr_id) ON DELETE SET NULL,
    assignated_team_id INTEGER
    REFERENCES team(digitalresource_ptr_id) ON DELETE SET NULL,
    assignation_date TIMESTAMPTZ NOT NULL DEFAULT now()
    );


-- --------------------------------------------------
-- 7) Archivos (hereda de Digital_Resource)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS file (
                                    digitalresource_ptr_id INTEGER PRIMARY KEY
                                    REFERENCES digital_resource(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL
    );


-- --------------------------------------------------
-- 8) Accesos de equipos a archivos (hereda)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS access (
                                      digitalresource_ptr_id INTEGER PRIMARY KEY
                                      REFERENCES digital_resource(id) ON DELETE CASCADE,
    accessed_file_id INTEGER
    REFERENCES file(digitalresource_ptr_id) ON DELETE SET NULL,
    accessing_team_id INTEGER
    REFERENCES team(digitalresource_ptr_id) ON DELETE SET NULL
    );


-- --------------------------------------------------
-- 9) Carpetas (hereda de Digital_Resource)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS folder (
                                      digitalresource_ptr_id INTEGER PRIMARY KEY
                                      REFERENCES digital_resource(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    parent_id INTEGER
    REFERENCES folder(digitalresource_ptr_id) ON DELETE SET NULL,
    path VARCHAR(255) NOT NULL
    );


-- --------------------------------------------------
-- 10) Ubicaciones (hereda de Digital_Resource)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS location (
                                        digitalresource_ptr_id INTEGER PRIMARY KEY
                                        REFERENCES digital_resource(id) ON DELETE CASCADE,
    located_file_id INTEGER
    REFERENCES file(digitalresource_ptr_id) ON DELETE SET NULL,
    located_project_id INTEGER
    REFERENCES project(digitalresource_ptr_id) ON DELETE SET NULL,
    located_folder_id INTEGER
    REFERENCES folder(digitalresource_ptr_id) ON DELETE SET NULL,
    path VARCHAR(1024) NOT NULL
    );


-- --------------------------------------------------
-- 11) Categorías (hereda de Digital_Resource)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS category (
                                        digitalresource_ptr_id INTEGER PRIMARY KEY
                                        REFERENCES digital_resource(id) ON DELETE CASCADE,
    label VARCHAR(50) NOT NULL UNIQUE
    );


-- --------------------------------------------------
-- 12) Clasificaciones (hereda de Digital_Resource)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS classification (
                                              digitalresource_ptr_id INTEGER PRIMARY KEY
                                              REFERENCES digital_resource(id) ON DELETE CASCADE,
    related_file_id INTEGER
    REFERENCES file(digitalresource_ptr_id) ON DELETE SET NULL,
    category_name_id INTEGER
    REFERENCES category(digitalresource_ptr_id) ON DELETE SET NULL
    );


-- --------------------------------------------------
-- 13) GeoJSON como subtipo de File (herencia doble)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS geojson (
                                       file_ptr_id INTEGER PRIMARY KEY
                                       REFERENCES file(digitalresource_ptr_id) ON DELETE CASCADE,
    content_type geojson_type_choices NOT NULL
    );


-- --------------------------------------------------
-- 14) Features de GeoJSON
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS geojson_feature (
                                               id SERIAL PRIMARY KEY,
                                               file_id INTEGER
                                               REFERENCES geojson(file_ptr_id) ON DELETE SET NULL,
    feature_type VARCHAR(50) NOT NULL,
    geometry Geometry
    );


-- --------------------------------------------------
-- 15) Atributos de propiedades
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS property_attribute (
                                                  id SERIAL PRIMARY KEY,
                                                  attribute_name VARCHAR(100) NOT NULL,
    attribute_type geojson_attr_type_choices NOT NULL
    );


-- --------------------------------------------------
-- 16) Valores de propiedades de Feature
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS geojson_feature_properties (
                                                          id SERIAL PRIMARY KEY,
                                                          feature_id INTEGER
                                                          REFERENCES geojson_feature(id) ON DELETE SET NULL,
    attribute_id INTEGER
    REFERENCES property_attribute(id) ON DELETE SET NULL,
    attribute_value VARCHAR(250)
    );



-- --------------------------------------------------
-- 17) Enumerado para GLOBAL_ROLES_CHOICES
-- --------------------------------------------------
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'global_roles_choices') THEN
CREATE TYPE global_roles_choices AS ENUM ('regular', 'superadmin');
END IF;
END$$;


-- --------------------------------------------------
-- 18) Tabla GlobalRole
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS global_role (
                                           id SERIAL PRIMARY KEY,
                                           name global_roles_choices NOT NULL
);


-- --------------------------------------------------
-- 19) Tabla GlobalMembership
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS global_membership (
                                                 id SERIAL PRIMARY KEY,
                                                 user_type_id INTEGER NOT NULL
                                                 REFERENCES global_role(id) ON DELETE SET DEFAULT,
    related_user_id INTEGER
    REFERENCES auth_user(id) ON DELETE SET NULL
    );



-- --------------------------------------------------
-- Rellenar la base de datos con datos de ejemplo
-- --------------------------------------------------

-- --------------------------------------------------
-- EJECUCIÓN DE INSERCIÓN DE DATOS DE EJEMPLO
-- --------------------------------------------------

-- 1) Crear usuario de prueba
DO $$
DECLARE
new_user_id INTEGER;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM auth_user WHERE username = 'creatorUser') THEN
    INSERT INTO public.auth_user (
      username, email, password, is_superuser, is_staff, is_active, date_joined
    )
    VALUES (
      'creatorUser',
      'creatorUser@example.com',
      -- REEMPLAZAR por hash real generado con make_password('jamon')
--       '$pbkdf2-sha256$260000$RANDOM_SALT_HERE$HASHED_PASSWORD_HERE',
     'jamon',
      false, false, true, NOW()
    )
    RETURNING id INTO new_user_id;
ELSE
SELECT id INTO new_user_id FROM auth_user WHERE username = 'creatorUser';
END IF;

  -- 2) Crear role 'creator'
  IF NOT EXISTS (
    SELECT 1 FROM public."role" r
    JOIN public."digital_resource" d ON r.digitalresource_ptr_id = d.id
    WHERE r.role_name = 'CREATOR' AND d.creator_id = new_user_id
  ) THEN
    WITH digital_resource AS (
      INSERT INTO public."digital_resource" (creator_id, created_at, deleted)
      VALUES (new_user_id, NOW(), false)
      RETURNING id
    )
    INSERT INTO public."role" (digitalresource_ptr_id, role_name)
    VALUES ((SELECT id FROM digital_resource), 'CREATOR');
END IF;

  -- 3) Crear equipo 'team_patata'
  IF NOT EXISTS (
    SELECT 1 FROM public."team" t
    JOIN public."digital_resource" d ON t.digitalresource_ptr_id = d.id
    WHERE t.name = 'team_patata' AND d.creator_id = new_user_id
  ) THEN
    WITH digital_resource AS (
      INSERT INTO public."digital_resource" (creator_id, created_at, deleted)
      VALUES (new_user_id, NOW(), false)
      RETURNING id
    )
    INSERT INTO public."team" (digitalresource_ptr_id, name)
    VALUES ((SELECT id FROM digital_resource), 'team_patata');
END IF;

  -- 4) Obtener IDs necesarios
  -- (Asegurarse de que solo haya un role y un team creados por este usuario)
  DECLARE
role_id INTEGER;
    team_id INTEGER;
BEGIN
SELECT r.digitalresource_ptr_id INTO role_id
FROM public."role" r
         JOIN public."digital_resource" d ON r.digitalresource_ptr_id = d.id
WHERE r.role_name = 'CREATOR' AND d.creator_id = new_user_id
    LIMIT 1;

SELECT t.digitalresource_ptr_id INTO team_id
FROM public."team" t
         JOIN public."digital_resource" d ON t.digitalresource_ptr_id = d.id
WHERE t.name = 'team_patata' AND d.creator_id = new_user_id
    LIMIT 1;

-- 5) Crear membership
IF NOT EXISTS (
      SELECT 1 FROM public."membership"
      WHERE member_id = new_user_id AND user_role_id = role_id AND user_team_id = team_id
    ) THEN
      WITH digital_resource AS (
        INSERT INTO public."digital_resource" (creator_id, created_at, deleted)
        VALUES (new_user_id, NOW(), false)
        RETURNING id
      )
      INSERT INTO public."membership" (digitalresource_ptr_id, member_id, user_role_id, user_team_id)
      VALUES ((SELECT id FROM digital_resource), new_user_id, role_id, team_id);
END IF;

    -- 6) Crear proyecto 'proyecto_cultivos_herbaceos'
    DECLARE
project_id INTEGER;
BEGIN
      IF NOT EXISTS (
        SELECT 1 FROM public."project" p
        JOIN public."digital_resource" d ON p.digitalresource_ptr_id = d.id
        WHERE p.name = 'proyecto_cultivos_herbaceos' AND d.creator_id = new_user_id
      ) THEN
        WITH digital_resource AS (
          INSERT INTO public."digital_resource" (creator_id, created_at, deleted)
          VALUES (new_user_id, NOW(), false)
          RETURNING id
        )
        INSERT INTO public."project" (digitalresource_ptr_id, name, active, finished)
        VALUES ((SELECT id FROM digital_resource), 'proyecto_cultivos_herbaceos', true, false);
END IF;

SELECT p.digitalresource_ptr_id INTO project_id
FROM public."project" p
         JOIN public."digital_resource" d ON p.digitalresource_ptr_id = d.id
WHERE p.name = 'proyecto_cultivos_herbaceos' AND d.creator_id = new_user_id
    LIMIT 1;

-- 7) Crear asignación
IF NOT EXISTS (
        SELECT 1 FROM public."assignations" a
        WHERE assignated_project_id = project_id AND assignated_team_id = team_id
      ) THEN
        WITH digital_resource AS (
          INSERT INTO public."digital_resource" (creator_id, created_at, deleted)
          VALUES (new_user_id, NOW(), false)
          RETURNING id
        )
        INSERT INTO public."assignations" (digitalresource_ptr_id, assignated_project_id, assignated_team_id, assignation_date)
        VALUES ((SELECT id FROM digital_resource), project_id, team_id, NOW());
END IF;

END;
END;
END$$;
