-- --------------------------------------------------
-- 0) ENUMs (sólo se crean si no existen)
-- --------------------------------------------------
DO
$$
DECLARE
BEGIN
    BEGIN
        CREATE TYPE roles_choices AS ENUM ('GUEST','VIEWER','CREATOR','OWNER','ADMIN');
        EXCEPTION
            WHEN duplicate_object THEN NULL;
    END;

    BEGIN
        CREATE TYPE geojson_type_choices AS ENUM ('Feature','FeatureCollection');
        EXCEPTION
            WHEN duplicate_object THEN NULL;
    END;

    BEGIN
        CREATE TYPE geojson_attr_type_choices AS ENUM ('int','float','str','bool');
        EXCEPTION
            WHEN duplicate_object THEN NULL;
    END;
END;
$$
LANGUAGE plpgsql;

-- --------------------------------------------------
-- 1) Tabla base de historial (DigitalResource)
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
-- 2) Equipos (hereda de DigitalResource)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS team (
                                    digitalresource_ptr_id INTEGER PRIMARY KEY
                                    REFERENCES digital_resource(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL
    );


-- --------------------------------------------------
-- 3) Roles (hereda de DigitalResource)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS role (
                                    digitalresource_ptr_id INTEGER PRIMARY KEY
                                    REFERENCES digital_resource(id) ON DELETE CASCADE,
    role_name roles_choices NOT NULL
    );


-- --------------------------------------------------
-- 4) Membership (hereda de DigitalResource)
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
-- 5) Proyectos (hereda de DigitalResource)
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
-- 7) Archivos (hereda de DigitalResource)
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
-- 9) Carpetas (hereda de DigitalResource)
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
-- 10) Ubicaciones (hereda de DigitalResource)
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
-- 11) Categorías (hereda de DigitalResource)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS category (
                                        digitalresource_ptr_id INTEGER PRIMARY KEY
                                        REFERENCES digital_resource(id) ON DELETE CASCADE,
    label VARCHAR(50) NOT NULL UNIQUE
    );


-- --------------------------------------------------
-- 12) Clasificaciones (hereda de DigitalResource)
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
