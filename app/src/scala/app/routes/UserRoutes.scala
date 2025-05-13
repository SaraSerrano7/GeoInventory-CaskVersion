package routes

import cask.MainRoutes
import ujson._

import java.io._
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import cask.{FormFile, FormValue, Response}
import sourcecode.Text.generate

import java.nio.file.{Files, Path, Paths}
//import org.apache.commons.fileupload._
//import org.apache.commons.fileupload.disk._
//import org.apache.commons.fileupload.util._
//import org.apache.commons.io.IOUtils

class UserRoutes extends cask.MainRoutes {
  @cask.get("/hello")
  def hello() = "Hola desde UserRoutes"

  @cask.get("/hello2")
  def hello2() = {
    "Hoooooooooooooooooola"
  }

  @cask.postForm("/upload-file")
  def upload_file(fileName: FormValue,
                  project: FormValue,
                  location: FormValue,
                  teams: Seq[FormValue] = Nil,
                  categories: Seq[FormValue] = Nil,
                  geojson_file: FormFile): cask.Response[String] = {
    try {

      // Extraemos bytes y nombre original
//      val bytes = geojson_file.bytes
      // 1) Leer el fichero temporal que Cask ya ha escrito
      val filePath: Path = geojson_file.filePath
        .getOrElse(return Response("""{"error":"no filePath in FormFile"}""", 400))
      val bytes: Array[Byte] =
        Files.readAllBytes(filePath)
      val originalName = geojson_file.fileName

      // 2) Hacer parse de los JSON-strings si los necesitas como listas
      val teamsList      = teams.map(_.value)
      val categoriesList = categories.map(_.value)


      Response(s"Archivo '$originalName' guardado como '${fileName.value}' en proyecto '${project.value}', ubicación '${location.value}'.", 200)

      //      val contentType = request.headers.getOrElse("content-type", "")

//      request.text()

//      val bodyBytes = request.readAllBytes()
//      val boundary = request.headers.get("content-type")
//        .flatMap(_.split("boundary=").toList.lift(1))
//        .getOrElse(throw new Exception("Missing boundary"))
//      val parsed = cask.util.MultipartParser
//        .parse(new java.io.ByteArrayInputStream(bodyBytes), boundary)
//      val parsed2 = request.textParams
//      val parsed3 = request.files
//
//      val fileName2 = request.multiParams.get("fileName").flatMap(_.headOption)
//        .getOrElse(return cask.Response("Missing fileName", 400))
//      val fileName3 = request.textParams.get("fileName").flatMap(_.headOption)
//        .getOrElse(return cask.Response("Falta fileName", 400))
//
//
//      val multiParams = request.multipartForm
//
//      def getParam(name: String) =
//        multiParams.get(name).flatMap(_.headOption).map(_.data)
//
//      val fileName = getParam("fileName").getOrElse(return cask.Response("Missing fileName", 400))
//      val fileProject = getParam("project").getOrElse(return cask.Response("Missing project", 400))
//      val fileLocation = getParam("location").getOrElse(return cask.Response("Missing location", 400))
//      val fileTeamsJson = getParam("teams").getOrElse("[]")
//      val fileCategoriesJson = getParam("categories").getOrElse("[]")
//
//      val file = multiParams.get("geojson_file").flatMap(_.headOption).getOrElse {
//        return cask.Response(ujson.Obj("error" -> "No se recibió un archivo").render(), 400)
//      }

    } catch {
      case e: ujson.ParseException =>
        Response(s"Error al parsear los datos: ${e.getMessage}", 500)

      case e: Exception =>
        Response(s"Error al procesar la subida: ${e.getMessage}", 500)

    }
  }

  initialize()

  /*

@require_http_methods(["POST"])
@transaction.atomic
def upload_file(request):
    try:
        file_name = request.POST.get("fileName")
        file_project = request.POST.get("project")
        file_location = request.POST.get("location")
        file_teams = request.POST.get("teams")
        file_categories = request.POST.get("categories")

        geojson_content = request.FILES["geojson_file"].read().decode("utf-8")

        if not geojson_content:
            return JsonResponse({"error": "No se recibió un GeoJSON válido"}, status=400)

        teams_list = json.loads(file_teams)
        categories_list = json.loads(file_categories)
        geojson_data = json.loads(geojson_content)

        # Create GeoJSONFile
        content_type = geojson_data['type']
        content_type_id = GEOJSON_TYPE_CHOICES.index((content_type, content_type))

        sql_queries(request_user_id=request.user.id,
                    content_type_id=content_type_id,
                    file_name=file_name,
                    teams_list=teams_list,
                    file_project=file_project,
                    file_location=file_location,
                    categories_list=categories_list,
                    geojson_data=geojson_data,
                    content_type=content_type)

        return JsonResponse({'status': 'success'}, status=200)
    except json.JSONDecodeError as e:
        return JsonResponse({"error": "El contenido no es un JSON válido", "details": str(e)}, status=400)
    except Exception as e:
        return JsonResponse({
            'status': 'error',
            'message': str(e)
        }, status=500)


def sql_queries(request_user_id, content_type_id, file_name, teams_list,
                file_project, file_location, categories_list,
                geojson_data, content_type):
    from django.conf import settings
    from django.db import connection

    with open('GeoInventory/config/db.conf') as db_file:
        credentials = db_file.read()

    cred_keys = [key.split('=') for key in credentials.split('\n')]
    keys = {}
    for key in cred_keys:
        if key != ['']:
            keys[key[0]] = key[1][1:-1]

    DB_CONFIG = {
        'host': settings.DATABASES['default']['HOST'] or 'localhost',
        'database': settings.DATABASES['default']['NAME'],
        'user': settings.DATABASES['default']['USER'],
        'password': settings.DATABASES['default']['PASSWORD'],
        'port': settings.DATABASES['default']['PORT'] or 5432,
    }

    try:
        current_user_object = None
        with connection.cursor() as cur:

            user_id = request_user_id if request_user_id else 1

            query_get_user = "SELECT * FROM public.auth_user WHERE id = %s;"
            cur.execute(query_get_user, [user_id])
            current_user_object = cur.fetchone()

            if not current_user_object:
                raise Exception("Usuario no encontrado")

            # 2. Crear registro en GeoJSON
            query_create_geojson = """
            WITH digital_resource AS (
                INSERT INTO public."Files_digitalresource" (creator_id, created_at, deleted)
                VALUES (%(creator_id)s, NOW(), false)
                RETURNING id
            ), file AS (
                INSERT INTO public."Files_file" (digitalresource_ptr_id, name)
                VALUES ((SELECT id FROM digital_resource), %(name)s)
                RETURNING digitalresource_ptr_id
            )
            INSERT INTO public."Files_geojson" (file_ptr_id, content_type)
            VALUES ((SELECT digitalresource_ptr_id FROM file), %(content_type)s)
            RETURNING file_ptr_id;
            """

            cur.execute(query_create_geojson, {
                'creator_id': current_user_object[0],
                'content_type': content_type_id,
                'name': file_name
            })
            geojson_file = cur.fetchone()
            geojson_file_id = geojson_file[0]

            geojson_fields = GeoJSON._meta.fields
            geojson_model_info = [(geojson_fields[i].name, attr) for i, attr in enumerate(geojson_file)]

            # 3. Obtener el objeto File asociado. Nota: innecesario, sql solo me devolverá el id
            # Suponiendo que la tabla file tiene el mismo id que geojson
            query_get_file = """SELECT * FROM public."Files_file" WHERE digitalresource_ptr_id = %(file_id)s;"""
            cur.execute(query_get_file, {'file_id': geojson_file_id})
            geojson_file_instance = cur.fetchone()
            if not geojson_file_instance:
                raise Exception("No se encontró el registro en file para el GeoJSON creado")

            # 4. Definir accesos para cada equipo en teams_list
            for team_name in teams_list:
                # 4.a Obtener el equipo
                query_get_team = """SELECT * FROM public."Files_team" WHERE name = %(team_name)s;"""
                cur.execute(query_get_team, {'team_name': team_name})
                team = cur.fetchone()
                if not team:
                    raise Exception(f"Equipo {team_name} no encontrado")

                # 4.b Crear el registro de acceso
                query_create_access = """
                    WITH digital_resource AS (
                        INSERT INTO public."Files_digitalresource" (creator_id, created_at, deleted)
                        VALUES (%(creator_id)s, NOW(), false)
                        RETURNING id
                    )

                    INSERT INTO public."Files_access" (digitalresource_ptr_id, accessed_file_id, accessing_team_id)
                    VALUES ((SELECT id FROM digital_resource), %(file_id)s, %(team_id)s);
                    """
                cur.execute(query_create_access, {
                    'creator_id': current_user_object[0],
                    'file_id': geojson_file_instance[0],
                    'team_id': team[0]
                })

            # 5. Obtener el proyecto a partir de file_project
            query_get_project = """SELECT * FROM public."Files_project" WHERE name = %(project_name)s;"""
            cur.execute(query_get_project, {'project_name': file_project})
            project = cur.fetchone()
            if not project:
                raise Exception("Proyecto no encontrado")

            # 6. Ubicar Folder y registrar la ubicación (Location)
            if file_location == file_project:
                # Si file_location equivale al nombre del proyecto
                query_create_location = """
                    WITH digital_resource AS (
                        INSERT INTO public."Files_digitalresource" (creator_id, created_at, deleted)
                        VALUES (%(creator_id)s, NOW(), false)
                        RETURNING id
                    )

                    INSERT INTO public."Files_location" (digitalresource_ptr_id, path, located_folder_id, located_project_id, located_file_id)
                    VALUES ((SELECT id FROM digital_resource), '', NULL, %(project_id)s, %(file_id)s);
                    """
                cur.execute(query_create_location, {
                    'creator_id': current_user_object[0],
                    'project_id': project[0],
                    'file_id': geojson_file_instance[0]
                })
            else:
                # TODO
                # Buscar si la carpeta ya existe
                query_get_folder = "SELECT * FROM public.Files_Folder WHERE path = %(folder_path)s;"
                cur.execute(query_get_folder, {'folder_path': file_location})
                folder = cur.fetchone()

                if not folder:
                    # Si la carpeta no existe, se determina si es de raíz o se necesita construir la ruta.
                    file_location_path = file_location.split('/')
                    if len(file_location_path) == 1 or (
                            len(file_location_path) == 2 and file_location_path[0] == file_project):
                        # Carpeta sin padre (root o subcarpeta inmediata)
                        # Si hay 2 elementos, usamos el segundo como nombre; si es 1, ese es el nombre.
                        name = file_location_path[1] if len(file_location_path) == 2 else file_location_path[0]
                        query_create_folder = """
                            INSERT INTO public.Files_Folder (name, parent_id, path)
                            VALUES (%(name)s, NULL, %(path)s)
                            RETURNING id;
                            """
                        cur.execute(query_create_folder, {'name': name, 'path': file_location})
                        folder = cur.fetchone()
                    else:
                        # Aquí se implementaría la lógica recursiva de 'build'
                        # Para efectos de ejemplo, se simula la creación de la carpeta intermedia
                        # Nota: Implementa la función build utilizando bucles y llamadas recursivas si es necesario
                        # En este ejemplo, se crea la carpeta final sin procesar la jerarquía.
                        query_create_folder = """
                            INSERT INTO public.Files_Folder (name, parent_id, path)
                            VALUES (%(name)s, NULL, %(path)s)
                            RETURNING id;
                            """
                        cur.execute(query_create_folder, {'name': file_location_path[-1], 'path': file_location})
                        folder = cur.fetchone()
                # Crear la ubicación con la carpeta identificada
                query_create_location = """
                    INSERT INTO public.Files_Location (located_folder_id, located_project_id, located_file_id)
                    VALUES (%(folder_id)s, %(project_id)s, %(file_id)s);
                    """
                cur.execute(query_create_location, {
                    'folder_id': folder['id'],
                    'project_id': project['id'],
                    'file_id': geojson_file_instance['id']
                })

            # 7. Clasificar el archivo si hay categorías
            if categories_list:
                for category_name in categories_list:
                    # Obtener la categoría
                    query_get_category = "SELECT * FROM category WHERE label = %(label)s;"
                    cur.execute(query_get_category, {'label': category_name})
                    category = cur.fetchone()
                    if not category:
                        raise Exception(f"Categoría {category_name} no encontrada")
                    # Crear la clasificación
                    query_create_classification = """
                        INSERT INTO classification (related_file_id, category_name_id)
                        VALUES (%(file_id)s, %(category_id)s);
                        """
                    cur.execute(query_create_classification, {
                        'file_id': geojson_file_id,
                        'category_id': category['id']
                    })


            # sustituimos los bucles por bulk_create
            bulk_insert_features_and_properties(cur, geojson_file_id, geojson_data)


        print("Operación completada exitosamente.")

    except Exception as e:
        print(f"Error: {e}")

    finally:
        if cur:
            cur.close()


def bulk_insert_features_and_properties(cur, geojson_file_id, geojson_data):
    from psycopg2.extras import execute_values
    features = []
    if geojson_data.get('type') == 'Feature':
        features = [geojson_data]
    else:
        features = geojson_data.get('features', [])

    feature_rows = []
    feature_shapes = []
    for feature in features:
        geometry = feature['geometry']
        geometry_type = geometry['type']
        feature_shape = shape(geometry)
        wkt = feature_shape.wkt
        feature_rows.append((geojson_file_id, geometry_type, wkt))
        feature_shapes.append((feature, wkt))  # Guardamos feature original + wkt para luego

    # Insertar todas las features
    feature_query = """
        INSERT INTO public."Files_geojsonfeature" (file_id, feature_type, geometry)
        VALUES %s
        RETURNING id;
    """

    feature_ids = execute_values(cur, feature_query, feature_rows, fetch=True)

    # execute_values(cur, feature_query, feature_rows)
    # feature_ids = [row[0] for row in cur.fetchall()]

    # Paso 2: recolectar todos los atributos únicos
    attributes_dict = {}  # key: (name, type) → id
    attribute_rows = []
    for feature, _ in feature_shapes:
        for key, value in feature.get('properties', {}).items():
            attr_type = type(value).__name__
            key_type = (key, attr_type)
            if key_type not in attributes_dict:
                attributes_dict[key_type] = None  # Placeholder
                attribute_rows.append((key, attr_type))

    # Insertar atributos únicos
    attribute_query = """
        INSERT INTO public."Files_propertyattribute" (attribute_name, attribute_type)
        VALUES %s
        RETURNING id, attribute_name, attribute_type;
    """
    execute_values(cur, attribute_query, attribute_rows)
    for attr_id, name, typ in cur.fetchall():
        attributes_dict[(name, typ)] = attr_id

    # Paso 3: relaciones feature ↔ atributo
    feature_property_rows = []
    for (feature, _), feature_id in zip(feature_shapes, feature_ids):
        for key, value in feature.get('properties', {}).items():
            attr_type = type(value).__name__
            attr_id = attributes_dict[(key, attr_type)]
            feature_property_rows.append((feature_id, attr_id, value))

    # Insertar todas las relaciones
    feature_prop_query = """
        INSERT INTO public."Files_geojsonfeatureproperties" (feature_id, attribute_id, attribute_value)
        VALUES %s;
    """
    execute_values(cur, feature_prop_query, feature_property_rows)

    return feature_ids

   */

}
