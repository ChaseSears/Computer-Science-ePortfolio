from pymongo import MongoClient, ASCENDING
from pymongo.errors import PyMongoError
from time import perf_counter


class AnimalShelter:
    """Database access layer for the AAC animal shelter MongoDB collection."""

    # Fields that must be present when a new animal is added.
    REQUIRED_FIELDS = {
        "animal_id",
        "animal_type",
        "breed",
        "sex_upon_outcome"
    }

    def __init__(self, username, password,
                 host="localhost",
                 port=27017,
                 database="aac",
                 collection="animals"):

        self.client = None
        self.database = None
        self.collection = None

        try:
            # Connect to MongoDB using the username and password.
            self.client = MongoClient(
                f"mongodb://{username}:{password}@"
                f"{host}:{port}/{database}?authSource=admin",
                serverSelectionTimeoutMS=5000
            )

            # Test the database connection.
            self.client.admin.command("ping")

            self.database = self.client[database]
            self.collection = self.database[collection]

            print("MongoDB connection successful.")

        except PyMongoError as error:
            print("Database connection error:", error)


    # ---------------------------------------------------------
    # Check that the database connection exists.
    # ---------------------------------------------------------
    def _ready(self):
        return self.collection is not None


    # ---------------------------------------------------------
    # DATA VALIDATION
    # ---------------------------------------------------------
    def validate_record(self, data, partial=False):

        if not isinstance(data, dict) or not data:
            return False, "Record must be a non-empty dictionary."

        # Update operations may use $set.
        values = data.get("$set", data)

        # New records must contain all required fields.
        if not partial:

            missing_fields = []

            for field in self.REQUIRED_FIELDS:

                if not str(values.get(field, "")).strip():
                    missing_fields.append(field)

            if missing_fields:
                return False, (
                    "Missing required fields: "
                    + ", ".join(sorted(missing_fields))
                )

        # Check that important text fields are not None.
        text_fields = (
            "animal_id",
            "animal_type",
            "breed",
            "sex_upon_outcome",
            "name",
            "location_lat",
            "location_long"
        )

        for field in text_fields:

            if field in values and values[field] is None:
                return False, field + " cannot be None."

        # Validate age.
        if "age_upon_outcome_in_weeks" in values:

            try:

                age = float(
                    values["age_upon_outcome_in_weeks"]
                )

                if age < 0:
                    return False, "Age cannot be negative."

            except (TypeError, ValueError):

                return False, "Age must be numeric."

        return True, ""


    # ---------------------------------------------------------
    # CREATE
    # ---------------------------------------------------------
    def create(self, data):

        if not self._ready():
            return False

        valid, message = self.validate_record(data)

        if not valid:
            print("Validation error:", message)
            return False

        try:

            result = self.collection.insert_one(data)

            return result.acknowledged

        except PyMongoError as error:

            print("Insert error:", error)

            return False


    # ---------------------------------------------------------
    # READ
    # ---------------------------------------------------------
    def read(self, query=None, projection=None,
             limit=0, sort=None):

        if not self._ready():
            return []

        if query is None:
            query = {}

        if not isinstance(query, dict):
            return []

        try:

            cursor = self.collection.find(
                query,
                projection
            )

            if sort:
                cursor = cursor.sort(sort)

            if limit:
                cursor = cursor.limit(limit)

            return list(cursor)

        except PyMongoError as error:

            print("Read error:", error)

            return []


    # ---------------------------------------------------------
    # UPDATE
    # ---------------------------------------------------------
    def update(self, query, update_data):

        if not self._ready():
            return 0

        if not isinstance(query, dict) or not query:
            return 0

        if not isinstance(update_data, dict) or not update_data:
            return 0

        # Allow either:
        # {"name": "Max"}
        #
        # or:
        # {"$set": {"name": "Max"}}

        if any(key.startswith("$") for key in update_data):

            update_document = update_data

        else:

            update_document = {
                "$set": update_data
            }

        valid, message = self.validate_record(
            update_document,
            partial=True
        )

        if not valid:

            print("Validation error:", message)

            return 0

        try:

            result = self.collection.update_many(
                query,
                update_document
            )

            return result.modified_count

        except PyMongoError as error:

            print("Update error:", error)

            return 0


    # ---------------------------------------------------------
    # DELETE
    # ---------------------------------------------------------
    def delete(self, query):

        if not self._ready():
            return 0

        # Do not allow an empty query.
        #
        # This prevents accidentally deleting every
        # animal in the database.
        if not isinstance(query, dict) or not query:

            print(
                "Delete rejected. "
                "A search condition is required."
            )

            return 0

        try:

            result = self.collection.delete_many(query)

            return result.deleted_count

        except PyMongoError as error:

            print("Delete error:", error)

            return 0


    # =========================================================
    # ENHANCEMENT 1
    # ADVANCED SEARCHING
    # =========================================================

    def build_search_query(
            self,
            breed=None,
            min_age=None,
            max_age=None,
            sex=None,
            location=None):

        query = {}

        # Search breed.
        #
        # Regex allows a partial search.
        # "lab" could match "Labrador Retriever".
        if breed:

            query["breed"] = {
                "$regex": breed.strip(),
                "$options": "i"
            }

        # Search by age range.
        age_query = {}

        if min_age not in (None, ""):

            age_query["$gte"] = float(min_age)

        if max_age not in (None, ""):

            age_query["$lte"] = float(max_age)

        if age_query:

            query[
                "age_upon_outcome_in_weeks"
            ] = age_query

        # Search by sex.
        if sex:

            query["sex_upon_outcome"] = sex

        # Search by location / placement information.
        if location:

            query["outcome_subtype"] = {
                "$regex": location.strip(),
                "$options": "i"
            }

        return query


    # ---------------------------------------------------------
    # Execute an advanced search.
    # ---------------------------------------------------------
    def search(
            self,
            breed=None,
            min_age=None,
            max_age=None,
            sex=None,
            location=None):

        try:

            query = self.build_search_query(
                breed,
                min_age,
                max_age,
                sex,
                location
            )

        except (TypeError, ValueError):

            print("Invalid search value.")

            return []

        return self.read(query)


    # =========================================================
    # ENHANCEMENT 2
    # DATABASE INDEXING
    # =========================================================

    def create_indexes(self):

        if not self._ready():
            return []

        try:

            indexes = []

            # Breed index
            indexes.append(
                self.collection.create_index(
                    [("breed", ASCENDING)],
                    name="idx_breed"
                )
            )

            # Age index
            indexes.append(
                self.collection.create_index(
                    [
                        (
                            "age_upon_outcome_in_weeks",
                            ASCENDING
                        )
                    ],
                    name="idx_age_weeks"
                )
            )

            # Sex index
            indexes.append(
                self.collection.create_index(
                    [
                        (
                            "sex_upon_outcome",
                            ASCENDING
                        )
                    ],
                    name="idx_sex"
                )
            )

            # Location index
            indexes.append(
                self.collection.create_index(
                    [
                        (
                            "outcome_subtype",
                            ASCENDING
                        )
                    ],
                    name="idx_location"
                )
            )

            # Compound index for searches that
            # combine multiple fields.
            indexes.append(
                self.collection.create_index(
                    [
                        ("breed", ASCENDING),

                        (
                            "age_upon_outcome_in_weeks",
                            ASCENDING
                        ),

                        (
                            "sex_upon_outcome",
                            ASCENDING
                        )
                    ],
                    name="idx_common_search"
                )
            )

            return indexes

        except PyMongoError as error:

            print("Index error:", error)

            return []


    # =========================================================
    # ENHANCEMENT 3
    # QUERY PERFORMANCE
    # =========================================================

    def explain_query(self, query):

        if not self._ready():
            return {}

        if not isinstance(query, dict):
            return {}

        try:

            results = self.database.command(
                "explain",
                {
                    "find": self.collection.name,
                    "filter": query
                },
                verbosity="executionStats"
            )

            return results

        except PyMongoError as error:

            print("Explain error:", error)

            return {}


    # ---------------------------------------------------------
    # Simple query timing test.
    # ---------------------------------------------------------
    def benchmark_query(self, query, runs=5):

        if not self._ready():
            return None

        if not isinstance(query, dict):
            return None

        if runs < 1:
            return None

        elapsed_times = []

        try:

            for run in range(runs):

                start = perf_counter()

                # Execute the query.
                list(
                    self.collection.find(query)
                )

                end = perf_counter()

                # Convert seconds to milliseconds.
                milliseconds = (
                    end - start
                ) * 1000

                elapsed_times.append(
                    milliseconds
                )

            average = (
                sum(elapsed_times)
                / len(elapsed_times)
            )

            return round(average, 3)

        except PyMongoError as error:

            print(
                "Benchmark error:",
                error
            )

            return None


    # =========================================================
    # ENHANCEMENT 4
    # MONGODB AGGREGATION REPORTING
    # =========================================================

    def aggregate_report(
            self,
            group_by="breed",
            match=None,
            limit=10):

        # Only allow known database fields.
        allowed_fields = {

            "breed":
                "breed",

            "sex":
                "sex_upon_outcome",

            "location":
                "outcome_subtype",

            "outcome":
                "outcome_type"
        }

        field = allowed_fields.get(
            group_by
        )

        if not self._ready():
            return []

        if field is None:
            return []

        pipeline = []

        # Apply the dashboard search filters
        # before generating the report.
        if match:

            pipeline.append(
                {
                    "$match": match
                }
            )

        # Group the records.
        pipeline.append(
            {
                "$group": {

                    "_id": {
                        "$ifNull": [
                            "$" + field,
                            "Unknown"
                        ]
                    },

                    "count": {
                        "$sum": 1
                    }
                }
            }
        )

        # Highest count first.
        pipeline.append(
            {
                "$sort": {
                    "count": -1
                }
            }
        )

        # Only return the top results.
        pipeline.append(
            {
                "$limit": int(limit)
            }
        )

        try:

            return list(
                self.collection.aggregate(
                    pipeline
                )
            )

        except PyMongoError as error:

            print(
                "Aggregation error:",
                error
            )

            return []