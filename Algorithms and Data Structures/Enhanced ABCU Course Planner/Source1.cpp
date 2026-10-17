//============================================================================
// Name        : Enhanced ABCU Course Planner.cpp
// Author      : Chase E. Sears (original); CS 499 enhancement draft
// Description : ABCU Course Planner - BST plus prerequisite graph
//============================================================================

#include <algorithm>
#include <cctype>
#include <fstream>
#include <iostream>
#include <map>
#include <queue>
#include <set>
#include <sstream>
#include <string>
#include <vector>

using namespace std;

// Structure used to store information about a course.
struct Course {
    string id, title;
    vector<string> prereqs;
};

// Node used by the binary search tree.
struct Node {
    Course data;
    Node* left = nullptr;
    Node* right = nullptr;

    explicit Node(const Course& c) : data(c) {}
};

// Binary search tree used to organize courses by course ID.
class CourseBST {

    Node* root = nullptr;

    // Insert a course into the tree.
    static Node* insertNode(Node* n, const Course& c) {

        if (!n)
            return new Node(c);

        if (c.id < n->data.id)
            n->left = insertNode(n->left, c);

        else if (c.id > n->data.id)
            n->right = insertNode(n->right, c);

        return n;
    }

    // Free all dynamically allocated nodes.
    static void destroy(Node* n) {

        if (!n)
            return;

        destroy(n->left);
        destroy(n->right);

        delete n;
    }

    // Print courses in alphanumeric order.
    static void inOrder(const Node* n) {

        if (!n)
            return;

        inOrder(n->left);

        cout << n->data.id << ", "
            << n->data.title << '\n';

        inOrder(n->right);
    }

public:

    CourseBST() = default;

    // Prevent unsafe copying of the tree.
    CourseBST(const CourseBST&) = delete;

    CourseBST& operator=(const CourseBST&) = delete;

    ~CourseBST() {
        Clear();
    }

    // Safely clear the tree before loading new information.
    void Clear() {
        destroy(root);
        root = nullptr;
    }

    // Insert a course.
    void Insert(const Course& c) {
        root = insertNode(root, c);
    }

    // Search for a course by its ID.
    const Course* Search(const string& id) const {

        const Node* n = root;

        while (n) {

            if (id == n->data.id)
                return &n->data;

            n = id < n->data.id
                ? n->left
                : n->right;
        }

        return nullptr;
    }

    // Display all courses in alphanumeric order.
    void PrintInOrder() const {

        cout << "Courses in alphanumeric order:\n";

        inOrder(root);

        cout << '\n';
    }
};

// Remove whitespace from the beginning and end of a string.
string trim(const string& s) {

    const size_t first =
        s.find_first_not_of(" \t\r\n");

    if (first == string::npos)
        return "";

    const size_t last =
        s.find_last_not_of(" \t\r\n");

    return s.substr(first, last - first + 1);
}

// Convert course IDs to uppercase.
string upper(string s) {

    for (char& c : s) {

        c = static_cast<char>(
            toupper(static_cast<unsigned char>(c))
            );
    }

    return s;
}

// Split a CSV line into individual fields.
vector<string> splitCSV(const string& line) {

    // The original project uses simple comma-delimited fields.
    // Quoted commas are not supported.

    vector<string> fields;

    stringstream stream(line);

    string field;

    while (getline(stream, field, ',')) {

        fields.push_back(trim(field));
    }

    if (!line.empty() && line.back() == ',') {

        fields.push_back("");
    }

    return fields;
}

//============================================================================
// Enhancement: Graph-Based Prerequisite Ordering
//
// A directed graph represents prerequisite relationships.
//
// Each edge points from a prerequisite to the course that requires it.
//
// Kahn's algorithm performs a topological sort to determine an order
// in which courses can be completed without violating prerequisites.
//
// If all courses cannot be processed, a circular dependency exists.
//============================================================================

bool courseOrder(
    const map<string, Course>& courses,
    vector<string>& order
) {

    // Store courses that depend on each prerequisite.
    map<string, vector<string>> dependents;

    // Store the number of prerequisites for each course.
    map<string, size_t> indegree;

    // Initialize prerequisite counts.
    for (const auto& entry : courses) {

        indegree[entry.first] = 0;
    }

    // Build the directed graph.
    for (const auto& entry : courses) {

        for (const string& pre : entry.second.prereqs) {

            dependents[pre].push_back(entry.first);

            ++indegree[entry.first];
        }
    }

    // Store courses that currently have no remaining prerequisites.
    // The priority queue keeps available courses in alphabetical order.
    priority_queue<
        string,
        vector<string>,
        greater<string>
    > ready;

    // Find courses with no prerequisites.
    for (const auto& entry : indegree) {

        if (entry.second == 0) {

            ready.push(entry.first);
        }
    }

    order.clear();

    // Process courses until no available courses remain.
    while (!ready.empty()) {

        string id = ready.top();

        ready.pop();

        // Add the course to the valid completion order.
        order.push_back(id);

        // Update courses that depend on this course.
        for (const string& next : dependents[id]) {

            // Remove one remaining prerequisite.
            --indegree[next];

            // Add the course when all prerequisites are satisfied.
            if (indegree[next] == 0) {

                ready.push(next);
            }
        }
    }

    // If every course was processed, no cycle exists.
    return order.size() == courses.size();
}

//============================================================================
// Enhancement: Improved File Loading and Validation
//
// Validate the entire file before replacing the current catalog.
//
// This prevents invalid files from destroying previously loaded data.
//============================================================================

bool LoadDataStructure(
    const string& filename,
    CourseBST& tree,
    map<string, Course>& courses
) {

    ifstream fin(filename);

    // Verify that the file can be opened.
    if (!fin) {

        cout << "Could not open file: "
            << filename << "\n\n";

        return false;
    }

    // Temporary storage for the new course information.
    map<string, Course> pending;

    string line;

    size_t lineNumber = 0;

    // Read the file one line at a time.
    while (getline(fin, line)) {

        ++lineNumber;

        // Skip empty lines.
        if (trim(line).empty())
            continue;

        vector<string> fields = splitCSV(line);

        // Validate the expected CSV format.
        if (
            fields.size() < 2 ||
            fields.size() > 4 ||
            fields[0].empty() ||
            fields[1].empty()
            ) {

            cout << "Invalid course record on line "
                << lineNumber << ".\n\n";

            return false;
        }

        Course c;

        c.id = upper(fields[0]);

        c.title = fields[1];

        // Reject duplicate course IDs.
        if (pending.count(c.id)) {

            cout << "Duplicate course ID "
                << c.id
                << " on line "
                << lineNumber
                << ".\n\n";

            return false;
        }

        // Track prerequisites to prevent duplicates.
        set<string> seen;

        // Process the prerequisite fields.
        for (size_t i = 2; i < fields.size(); ++i) {

            string pre = upper(fields[i]);

            if (pre.empty())
                continue;

            // Reject self-references and repeated prerequisites.
            if (
                pre == c.id ||
                !seen.insert(pre).second
                ) {

                cout << "Invalid or repeated prerequisite for "
                    << c.id
                    << ".\n\n";

                return false;
            }

            c.prereqs.push_back(pre);
        }

        // Add the validated course to temporary storage.
        pending.emplace(c.id, c);
    }

    // Reject files that contain no courses.
    if (pending.empty()) {

        cout << "File contains no courses.\n\n";

        return false;
    }

    // Verify that every prerequisite exists in the catalog.
    for (const auto& entry : pending) {

        for (const string& pre : entry.second.prereqs) {

            if (!pending.count(pre)) {

                cout << "Unknown prerequisite "
                    << pre
                    << " for "
                    << entry.first
                    << ".\n\n";

                return false;
            }
        }
    }

    // Check for circular prerequisite relationships.
    vector<string> order;

    if (!courseOrder(pending, order)) {

        cout << "Circular prerequisite dependency detected; "
            << "catalog not loaded.\n\n";

        return false;
    }

    // All validation has passed.
    // Safely replace the existing catalog.
    tree.Clear();

    courses.swap(pending);

    // Insert courses into the binary search tree.
    for (const auto& entry : courses) {

        tree.Insert(entry.second);
    }

    cout << "Loaded "
        << courses.size()
        << " courses successfully.\n\n";

    return true;
}

//============================================================================
// Print information about a selected course.
//============================================================================

void PrintCourseInfo(
    const CourseBST& tree,
    const string& rawId
) {

    string id = upper(trim(rawId));

    const Course* c = tree.Search(id);

    // Handle a course that does not exist.
    if (!c) {

        cout << "Course "
            << id
            << " was not found.\n\n";

        return;
    }

    // Print the course ID and title.
    cout << c->id
        << ", "
        << c->title
        << "\nPrerequisites: ";

    // Print prerequisite information.
    if (c->prereqs.empty()) {

        cout << "None";
    }

    else {

        for (size_t i = 0; i < c->prereqs.size(); ++i) {

            cout << (i ? ", " : "")
                << c->prereqs[i];
        }
    }

    cout << "\n\n";
}

//============================================================================
// Main Program
//============================================================================

int main() {

    // Binary search tree containing the courses.
    CourseBST catalog;

    // Map used to support graph operations.
    map<string, Course> courses;

    cout << "Welcome to the ABCU course planner.\n\n";

    // Continue displaying the menu until the user exits.
    while (true) {

        cout << "  1. Load Data Structure\n"
            << "  2. Print Course List\n"
            << "  3. Print Course\n"
            << "  4. Print Prerequisite Order\n"
            << "  9. Exit\n\n"
            << "What would you like to do? ";

        string choice;

        if (!getline(cin, choice))
            break;

        choice = trim(choice);

        // Exit the program.
        if (choice == "9") {

            cout << "Thank you for using the course planner!\n";

            break;
        }

        // Option 1: Load course information.
        if (choice == "1") {

            cout << "Enter course data filename: ";

            string filename;

            if (!getline(cin, filename))
                break;

            if (trim(filename).empty()) {

                cout << "Filename cannot be empty.\n\n";

                continue;
            }

            // A failed reload leaves the previous valid catalog available.
            LoadDataStructure(
                trim(filename),
                catalog,
                courses
            );
        }

        // Options 2, 3, and 4 require loaded course information.
        else if (
            choice == "2" ||
            choice == "3" ||
            choice == "4"
            ) {

            if (courses.empty()) {

                cout << "Please load the data first "
                    << "with option 1.\n\n";

                continue;
            }

            // Option 2: Print courses alphabetically.
            if (choice == "2") {

                catalog.PrintInOrder();
            }

            // Option 3: Search for an individual course.
            else if (choice == "3") {

                cout << "What course do you want to know about? ";

                string id;

                if (!getline(cin, id))
                    break;

                if (trim(id).empty()) {

                    cout << "Course ID cannot be empty.\n\n";
                }

                else {

                    PrintCourseInfo(catalog, id);
                }
            }

            // Option 4: Print courses in prerequisite order.
            else {

                vector<string> order;

                if (courseOrder(courses, order)) {

                    cout << "One valid prerequisite-respecting "
                        << "course order:\n";

                    for (const string& id : order) {

                        cout << id
                            << ", "
                            << courses.at(id).title
                            << '\n';
                    }

                    cout << '\n';
                }

                else {

                    cout << "Cannot produce an order: "
                        << "circular dependency.\n\n";
                }
            }
        }

        // Handle invalid menu selections.
        else {

            cout << choice
                << " is not a valid option.\n\n";
        }
    }

    return 0;
}