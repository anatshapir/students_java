# JavaEdu Teacher Manual

This manual covers how to use the JavaEdu platform as a teacher.

## Getting Started

### Logging In

1. Navigate to the teacher dashboard URL
2. Enter your email and password
3. Click "Login"

If you don't have an account, contact your administrator.

### Dashboard Overview

The dashboard shows:
- **Total Courses**: Number of courses you teach
- **Total Students**: Combined enrollment across all courses
- **Total Exercises**: All exercises you've created
- **Recent Submissions**: Submissions in the last 7 days

## Managing Courses

### Creating a Course

1. Courses are created by administrators
2. Contact your admin to have a course created

### Viewing Students

1. Go to **Students** in the navigation
2. Select a course from the dropdown
3. View enrolled students and their progress

### Enrolling Students

Students can:
- Self-enroll using a course code
- Be imported from Google Classroom
- Be imported from a GitHub organization

## Creating Exercises

### Basic Exercise Creation

1. Go to **Exercises** > **Create Exercise**
2. Fill in the required fields:
   - **Course**: Select the target course
   - **Title**: Clear, descriptive name
   - **Description**: Explain what students need to do
   - **Difficulty**: BEGINNER, EASY, MEDIUM, HARD, or EXPERT
   - **Points**: Maximum points possible

### Writing Good Descriptions

- Clearly state the problem
- Provide input/output examples
- List any constraints
- Mention which concepts are being tested

Example:
```
Write a method that finds the maximum value in an array.

Method signature:
public static int findMax(int[] arr)

Example:
Input: [3, 1, 4, 1, 5, 9, 2, 6]
Output: 9

Constraints:
- Array will have at least one element
- Array values are between -1000 and 1000
```

### Starter Code

Provide code that students should start with:

```java
public class Solution {
    public static int findMax(int[] arr) {
        // Your code here
        return 0;
    }

    public static void main(String[] args) {
        // Test your solution
        int[] test = {3, 1, 4, 1, 5, 9, 2, 6};
        System.out.println(findMax(test)); // Should print 9
    }
}
```

### Creating Test Cases

Test cases verify student solutions automatically.

#### Visible Test Cases
- Students can see these before submitting
- Help students understand requirements
- Show expected input/output

#### Hidden Test Cases
- Students cannot see these
- Test edge cases
- Prevent hardcoding solutions

#### Test Code Format

Write Java code that tests the student's solution:

```java
// Test basic functionality
Solution sol = new Solution();
assert sol.findMax(new int[]{1, 2, 3}) == 3;

// Test with negative numbers
assert sol.findMax(new int[]{-5, -2, -8}) == -2;

// Test single element
assert sol.findMax(new int[]{42}) == 42;
```

### Adding Hints

Create hints to help struggling students:

1. Start with general guidance
2. Progress to more specific help
3. Never give away the solution

Example hint sequence:
1. "Think about how you would find the maximum by hand"
2. "You'll need to keep track of the largest value seen so far"
3. "Initialize your maximum with the first element, then compare each element"

### Publishing Exercises

- Exercises are **drafts** by default
- Click the status badge to toggle **Published**
- Only published exercises are visible to students

## Viewing Analytics

### Course Analytics

1. Go to **Analytics**
2. Select a course
3. View:
   - Submissions over time
   - Exercise completion rates
   - Common errors
   - Students needing help

### Exercise Analytics

Each exercise shows:
- **Total Submissions**: All submission attempts
- **Unique Students**: How many students attempted
- **Completion Rate**: Percentage who passed
- **Average Attempts**: How many tries to succeed
- **Common Errors**: Frequent mistakes

### Identifying Struggling Students

The system automatically identifies students who:
- Have many incomplete exercises
- Have excessive attempts without success
- Haven't submitted in a while

Consider:
- Reaching out to offer help
- Reviewing if exercises are too difficult
- Adding more hints

## Grading

### Automatic Grading

- Submissions are graded automatically based on test results
- Score = points from passed tests / total possible points
- Feedback shows which tests passed/failed

### Manual Grading

For exercises needing human review:
1. View the submission
2. Add feedback comments
3. Adjust the score if needed
4. Save changes

## Integrations

### Google Classroom

1. Link your course to Google Classroom
2. Import student roster automatically
3. Export grades back to Classroom

### GitHub

1. Connect to a GitHub organization
2. Import organization members as students
3. Optionally track commits per student

## Best Practices

### Exercise Design

- Start with easier exercises
- Build complexity gradually
- Test your own exercises before publishing
- Review common errors and add hints

### Time Management

- Set reasonable due dates
- Use the dashboard to monitor progress
- Address struggling students early

### Communication

- Use exercise descriptions clearly
- Provide meaningful feedback
- Update hints based on common errors

## Troubleshooting

### Students can't see exercises
- Check if exercises are published
- Verify students are enrolled in the course

### Tests passing incorrectly
- Review test code for bugs
- Check edge cases
- Verify expected outputs

### Analytics not updating
- Analytics update daily
- Recent submissions may take time to appear
