package com.edt.ut3.backend.formation_choice

import android.net.Uri
import androidx.core.net.toUri
import com.edt.ut3.R
import com.edt.ut3.backend.formation_choice.School.Info
import com.edt.ut3.misc.extensions.map
import com.edt.ut3.misc.extensions.toJSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents a School by its name and [information][Info].
 *
 * @property name
 * @property info
 */
data class School(
    val name: String,
    val info: List<Info>
){
    companion object {
        /**
         * Parse an [School] object from a JSON string.
         *
         * @throws JSONException If the json representation isn't valid.
         * @param json The JSON object to parse.
         */
        @Throws(JSONException::class)
        fun fromJSON(json: JSONObject) = School (
            name = json.getString("name"),
            info = json.getJSONArray("infos").map {
                Info.fromJSON(it as JSONObject)
            }
        )

        /**
         * Returns the default School which is Paul Sabatier
         * with all its [informations][Info].
         */
        val default = School(
            name="Université Paul Sabatier",
            info = listOf(
                Info(
                    name="FSI",
                    url="https://edt.univ-tlse3.fr/calendar2",
                    groups="https://edt.univ-tlse3.fr/calendar2/Home/ReadResourceListItems?myResources=false&searchTerm=___&pageSize=10000&pageNumber=1&resType=103&_=1601408259547",
                    rooms="https://edt.univ-tlse3.fr/calendar2/Home/ReadResourceListItems?myResources=false&searchTerm=___&pageSize=10000&pageNumber=1&resType=102&_=1601408259546",
                    courses="https://edt.univ-tlse3.fr/calendar2/Home/ReadResourceListItems?myResources=false&searchTerm=___&pageSize=10000&pageNumber=1&resType=100&_=1601408259545"
                )
            )
        )
    }

    /**
     * Serialize the current [object][School]
     * into a JSONObject.
     */
    fun toJSON() = JSONObject().apply {
        put("name", name)
        put("infos", info.toJSONArray { it.toJSON() })
    }

    /**
     * Represent a [school][School] information.
     *
     * @property name The faculty name
     * @property url The faculty schedule url
     * @property groups The link to get all the groups
     * @property rooms The link to get all the rooms
     * @property courses The link to get all the courses
     */
    data class Info (
        val name: String,
        val url: String,
        val groups: String,
        val rooms: String,
        val courses: String
    ){
        companion object {
            /**
             * Parse an [Info] object from a JSON string.
             *
             * @throws JSONException If the json representation isn't valid.
             * @param json The JSON object to parse.
             */
            @Throws(JSONException::class)
            fun fromJSON(json: JSONObject) = Info (
                name = json.getString("name"),
                url = json.getString("url"),
                groups = json.getString("groups"),
                rooms = json.getString("rooms"),
                courses = json.getString("courses")
            )

            /**
             * This function extracts the fids from a Celcat URL.
             *
             * @param uri The celcat url
             * @return All found fids.
             */
            private fun extractFids(uri: Uri): List<String> {
                /**
                 * Extract the fids until the next fid
                 * is null or blank.
                 *
                 * @param index The current index (default: 0)
                 * @return The fid list
                 */
                fun extract(index: Int = 0): List<String> {
                    val fid = uri.getQueryParameter("fid$index")
                    if (fid.isNullOrBlank()) {
                        return listOf()
                    }

                    return extract(index + 1) + fid
                }

                return extract()
            }



            val celcatLinkPattern = Regex("(.*)/cal.*")
            /**
             * Try to converts a classic link to and [Info].
             * The link must match [this pattern][celcatLinkPattern]
             *
             * @InvalidLinkException If the link isn't valid. It contains the error
             * as an ID which is traduced in several languages.
             * @param link The link to parse
             * @return A pair containing an [Info] and a list of fids.
             */
            @Throws(InvalidLinkException::class)
            fun fromClassicLink(link: String): Pair<Info, List<String>> {
                try {
                    try {
                        val baseLink = celcatLinkPattern.find(link)?.value
                        val fids = extractFids(link.toUri())

                        if (baseLink.isNullOrBlank()) {
                            throw InvalidLinkException(R.string.error_invalid_link)
                        }

                        if (fids.isEmpty()) {
                            throw InvalidLinkException(R.string.error_link_groups)
                        }

                        val name = ""
                        println(baseLink)
                        val url = celcatLinkPattern.find(link)?.groups?.get(1)?.value!!
                        val groups = guessGroupsLink(url)
                        val rooms = guessRoomsLink(url)
                        val courses = guessCoursesLink(url)

                        return Pair(Info(name, url, groups, rooms, courses), fids)
                    } catch (e: UnsupportedOperationException) {
                        throw InvalidLinkException(R.string.error_invalid_link)
                    }
                } catch (e: InvalidLinkException) {
                    throw e
                }
            }

            /**
             * Tries to guess the link to retrieve all the groups.
             *
             * @param link The base link
             * @return The group link
             */
            private fun guessGroupsLink(link: String): String {
                val search =
                    if (link.contains("calendar2")) { "___" }
                    else { "__" }

                return "$link/Home/ReadResourceListItems?myResources=false&searchTerm=$search&pageSize=1000000&pageNumber=1&resType=103"
            }

            /**
             * Tries to guess the link to retrieve all the rooms.
             *
             * @param link The base link
             * @return The rooms link
             */
            private fun guessRoomsLink(link: String): String {
                val search =
                    if (link.contains("calendar2")) { "___" }
                    else { "__" }

                return "$link/Home/ReadResourceListItems?myResources=false&searchTerm=$search&pageSize=1000000&pageNumber=1&resType=102"
            }

            /**
             * Tries to guess the link to retrieve all the courses.
             *
             * @param link The base link
             * @return The courses link
             */
            private fun guessCoursesLink(link: String): String {
                val search =
                    if (link.contains("calendar2")) { "___" }
                    else { "__" }

                return "$link/Home/ReadResourceListItems?myResources=false&searchTerm=$search&pageSize=10000000&pageNumber=1&resType=100"
            }
        }

        /**
         * Serialize the current [object][Info]
         * into a JSONObject.
         */
        fun toJSON() = JSONObject().apply {
            put("name", name)
            put("url", url)
            put("groups", groups)
            put("rooms", rooms)
            put("courses", courses)
        }

        /**
         * Thrown if the given link is invalid.
         *
         * @property reason A resource id pointing to
         * the current error. (Can be used to display
         * errors to the final user ).
         */
        class InvalidLinkException(val reason: Int): Exception()

        /**
         * Represent a faculty group.
         *
         * @property id The group id
         * @property text The textual representation
         */
        data class Group (
            val id: String,
            val text: String
        ) {
            companion object {
                /**
                 * Parse a [Group] object from a JSON string.
                 *
                 * @throws JSONException If the json representation isn't valid.
                 * @param json The JSON object to parse.
                 */
                @Throws(JSONException::class)
                fun fromJSON(json: JSONObject) = Group (
                    id = json.getString("id"),
                    text = json.getString("text")
                )
            }

            /**
             * Serialize the current [object][Group]
             * into a JSONObject.
             */
            fun toJSON() = JSONObject().apply {
                put("id", id)
                put("text", text)
            }
        }
    }
}