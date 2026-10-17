//============================================================================
// Name        : ProjectTwo.cpp
// Author      : Chase E. Sears
// Version     : 1.0
// Description : ABCU Advising Assistance Program
//============================================================================

#include <fstream>
#include <iostream>
#include <string>
#include <vector>

using namespace std;

// Global definitions visible to all methods and classes
// structure for a course
struct Course {
    string id;                 // like CSCI200
    string title;              // course name
    vector<string> prereqs;    // 0, 1, or 2 prereqs
    Course() {}
    Course(const string& cid, const string& ctitle) {
        id = cid;
        title = ctitle;
    }
};

// node used by the BST
struct Node {
    Course data;
    Node* left;
    Node* right;
    Node(const Course& c) {
        data = c;
        left = nullptr;
        right = nullptr;
    }
};

// Binary Search Tree for courses (key = course id)
class CourseBST {

private:
    Node* root;

    // insert course into tree
    Node* insertNode(Node* node, const Course& c) {
        if (node == nullptr) {
            return new Node(c);
        }
        if (c.id < node->data.id) {
            node->left = insertNode(node->left, c);
        }
        else {
            node->right = insertNode(node->right, c);
        }
        return node;
    }

    // in-order print
    void inOrder(Node* node) {
        if (node != nullptr) {
            inOrder(node->left);
            cout << node->data.id << ", " << node->data.title << endl;
            inOrder(node->right);
        }
    }

    // free memory (post-order)
    void destroy(Node* node) {
        if (node != nullptr) {
            destroy(node->left);
            destroy(node->right);
            delete node;
        }
    }

    // search by id
    Node* find(Node* node, const string& id) const {
        if (node == nullptr) return nullptr;
        if (id == node->data.id) return node;
        if (id < node->data.id) return find(node->left, id);
        return find(node->right, id);
    }

public:
    CourseBST() { root = nullptr; }
    ~CourseBST() { destroy(root); root = nullptr; }

    void Insert(const Course& c) { root = insertNode(root, c); }

    void PrintInOrder() {
        cout << "Here is a sample schedule:" << endl;
        inOrder(root);
        cout << endl;
    }

    const Course* Search(const string& id) const {
        Node* n = find(root, id);
        if (n == nullptr) return nullptr;
        return &n->data;
    }
};

// make uppercase so "csc400" works
string toUpperCopy(string s) {
    for (size_t i = 0; i < s.size(); ++i) {
        s[i] = static_cast<char>(toupper(static_cast<unsigned char>(s[i])));
    }
    return s;
}

// get CSV field by index using find/substr (0-based)
string getCSVField(const string& line, int fieldIndex) {
    size_t start = 0;
    int current = 0;
    while (true) {
        size_t comma = line.find(',', start);
        if (current == fieldIndex) {
            if (comma == string::npos) return line.substr(start);
            return line.substr(start, comma - start);
        }
        if (comma == string::npos) return "";
        start = comma + 1;
        current++;
    }
}

// only add prereq if not empty
void pushIfNotEmpty(const string& token, vector<string>& out) {
    if (token.size() != 0) out.push_back(token);
}

// Loading and printing logic that uses the BST
// load courses from file and insert into BST
// expected format: COURSE_ID,COURSE_TITLE,PREREQ1,PREREQ2
bool LoadDataStructure(const string& filename, CourseBST& tree, vector<Course>& loaded) {
    ifstream fin(filename);
    if (!fin) {
        cout << "Could not open file " << filename << endl << endl;
        return false;
    }

    loaded.clear();

    string line;
    while (getline(fin, line)) {
        if (line.size() == 0) continue;

        string id = getCSVField(line, 0);
        string title = getCSVField(line, 1);
        string p1 = getCSVField(line, 2);
        string p2 = getCSVField(line, 3);

        if (id.size() == 0 || title.size() == 0) continue;

        Course c(id, title);
        pushIfNotEmpty(p1, c.prereqs);
        pushIfNotEmpty(p2, c.prereqs);

        tree.Insert(c);
        loaded.push_back(c);
    }

    fin.close();
    return true;
}

// print one course and its prereqs
void PrintCourseInfo(const CourseBST& tree, const string& rawId) {
    string id = toUpperCopy(rawId);
    const Course* c = tree.Search(id);
    if (c == nullptr) {
        cout << "Course " << id << " was not found" << endl << endl;
        return;
    }

    cout << c->id << ", " << c->title << endl;

    if (c->prereqs.size() == 0) {
        cout << "Prerequisites: None" << endl << endl;
    }
    else {
        cout << "Prerequisites: ";
        for (size_t i = 0; i < c->prereqs.size(); ++i) {
            cout << c->prereqs[i];
            if (i + 1 < c->prereqs.size()) cout << ", ";
        }
        cout << endl << endl;
    }
}

// main()
int main() {
    CourseBST catalog;           // BST with all courses
    vector<Course> loaded;       // optional copy of loaded courses

    cout << "Welcome to the course planner." << endl << endl;

    // ask for filename first
    cout << "Enter course data filename: ";
    string filename;
    getline(cin, filename);

    bool loadedOk = false;
    if (filename.size() != 0) {
        loadedOk = LoadDataStructure(filename, catalog, loaded);
    }

    int choice = 0;
    while (choice != 9) {
        cout << "  1. Load Data Structure." << endl;
        cout << "  2. Print Course List." << endl;
        cout << "  3. Print Course." << endl;
        cout << "  9. Exit" << endl;
        cout << endl;
        cout << "What would you like to do? ";

        string line;
        if (!getline(cin, line)) break;

        // check if they typed a single digit
        if (line.size() == 1 && isdigit(static_cast<unsigned char>(line[0]))) {
            choice = line[0] - '0';

            // if it's a number but not one of the valid options, say so and loop
            if (!(choice == 1 || choice == 2 || choice == 3 || choice == 9)) {
                cout << choice << " is not a valid option." << endl << endl;
                continue;
            }
        }
        else {
            // not a number (or more than one char) -> repeat what they typed
            cout << line << " is not a valid option." << endl << endl;
            continue;
        }

        switch (choice) {
        case 1: {
            cout << "Enter course data filename: ";
            string fname;
            getline(cin, fname);
            if (fname.size() == 0) {
                cout << "Filename cannot be empty" << endl << endl;
                break;
            }

            // reset to a fresh empty tree before reloading
            catalog = CourseBST();
            loaded.clear();

            bool ok = LoadDataStructure(fname, catalog, loaded);
            loadedOk = ok;
            cout << endl;
            break;
        }

        case 2: {
            if (!loadedOk) {
                cout << "Please load the data first with option 1." << endl << endl;
                break;
            }
            catalog.PrintInOrder();
            break;
        }

        case 3: {
            if (!loadedOk) {
                cout << "Please load the data first with option 1." << endl << endl;
                break;
            }
            cout << "What course do you want to know about? ";
            string q;
            getline(cin, q);
            if (q.size() == 0) {
                cout << "Course id cannot be empty" << endl << endl;
                break;
            }
            PrintCourseInfo(catalog, q);
            break;
        }

        case 9:
            cout << "Thank you for using the course planner!" << endl;
            break;
        }
    }

    return 0;
}
