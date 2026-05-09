# Testing

## Core Principles

1. **Tests enable change** - Without tests, code rots
2. **Clean tests** - Test code is as important as production code
3. **One concept per test** - Each test verifies one thing
4. **F.I.R.S.T.** - Fast, Independent, Repeatable, Self-Validating, Timely

## The Three Laws of TDD

1. **Don't write production code** until you have a failing test
2. **Don't write more test** than needed to fail
3. **Don't write more production code** than needed to pass the test

Following these laws creates a rapid cycle of 30-second iterations.

## Keep Tests Clean

**Test code is just as important as production code.** It requires thought, design, and care.

```python
# Bad - hard to understand, brittle
def test_get_page_hierarchy():
    c = crawler.get_page("PageOne")
    p = c.get_parent()
    assert p.get_name() == "root"

# Good - readable, clear intent
def test_get_page_hierarchy():
    make_pages("PageOne", "PageOne.ChildOne")
    
    page_one = crawler.get_page("PageOne")
    parent = page_one.get_parent()
    
    assert parent.get_name() == "root"
```

### Tests Enable Change

Without tests:
- Code rots and degrades
- Fear of change paralyzes development
- Architecture deteriorates

With clean tests:
- You can refactor confidently
- Code stays flexible
- Architecture improves

## Clean Test Structure: BUILD-OPERATE-CHECK

Every test should have three parts:

```python
def test_user_registration():
    # BUILD - Set up test data
    user_data = {
        'name': 'John Doe',
        'email': 'john@example.com',
        'age': 30
    }
    
    # OPERATE - Execute the operation
    result = register_user(user_data)
    
    # CHECK - Verify the result
    assert result.success is True
    assert result.user.name == 'John Doe'
```

## One Concept Per Test

Each test should verify one concept:

```python
# Bad - tests multiple concepts
def test_user_operations():
    user = create_user("John")
    assert user.name == "John"  # Creation
    
    user.set_email("john@example.com")
    assert user.email == "john@example.com"  # Email setting

# Good - one concept each
def test_user_creation():
    user = create_user("John")
    assert user.name == "John"

def test_user_email_can_be_set():
    user = create_user("John")
    user.set_email("john@example.com")
    assert user.email == "john@example.com"
```

## Domain-Specific Testing Language

Build helper functions that make tests readable:

```python
# Bad - low-level details in every test
def test_page_hierarchy():
    crawler = PageCrawler()
    page = crawler.add_page("root", PathParser.parse("PageOne"))
    page.add_attribute("Test", "true")
    # ... many more lines

# Good - domain-specific test language
def test_page_hierarchy():
    given_pages("PageOne", "PageOne.ChildOne")
    when_requested("PageOne")
    then_response_contains("PageOne", "ChildOne")

# Helper functions
def given_pages(*page_names):
    for name in page_names:
        make_page(name)

def when_requested(page_name):
    global response
    response = request_page(page_name)

def then_response_contains(*expected_content):
    for content in expected_content:
        assert content in response.get_content()
```

## F.I.R.S.T. Principles

### Fast

Tests should run quickly. Slow tests won't be run frequently.

```python
# Bad - slow test
def test_user_processing():
    time.sleep(5)  # Simulating slow operation
    # ...

# Good - fast test with mocks
def test_user_processing():
    with mock.patch('time.sleep'):
        # ...
```

### Independent

Tests should not depend on each other:

```python
# Bad - dependent tests
class TestUserFlow:
    user = None
    
    def test_1_create_user(self):
        self.user = create_user("John")
    
    def test_2_update_user(self):
        # Depends on test_1!
        self.user.set_email("john@example.com")

# Good - independent tests
def test_create_user():
    user = create_user("John")
    assert user is not None

def test_update_user():
    user = create_user("John")  # Set up own data
    user.set_email("john@example.com")
    assert user.email == "john@example.com"
```

### Repeatable

Tests should produce same results every time:

```python
# Bad - depends on external state
def test_get_current_user():
    user = get_current_logged_in_user()
    assert user.name == "John"

# Good - controlled state
def test_get_current_user():
    with logged_in_as("John"):
        user = get_current_logged_in_user()
        assert user.name == "John"
```

### Self-Validating

Tests should have a boolean output: pass or fail:

```python
# Bad - requires manual inspection
def test_render_page():
    html = render_page()
    print(html)  # Developer must check

# Good - automatic validation
def test_render_page():
    html = render_page()
    assert "<html>" in html
    assert "<body>" in html
```

### Timely

Write tests just before production code (TDD).

## Common Testing Patterns

### Test Fixtures

Set up common test data:

```python
import pytest

@pytest.fixture
def sample_user():
    return User(name="John Doe", email="john@example.com")

@pytest.fixture
def database():
    db = Database()
    db.connect()
    yield db
    db.disconnect()

def test_save_user(sample_user, database):
    database.save(sample_user)
    assert database.count() == 1
```

### Mocking External Dependencies

```python
from unittest.mock import Mock, patch

def test_send_email():
    email_service = Mock()
    
    user_service = UserService(email_service)
    user_service.register_user({"email": "test@example.com"})
    
    email_service.send.assert_called_once()

@patch('requests.get')
def test_fetch_data(mock_get):
    mock_get.return_value.json.return_value = {'data': 'test'}
    
    result = fetch_external_data()
    
    assert result['data'] == 'test'
```

### Parameterized Tests

Test multiple inputs without duplication:

```python
import pytest

@pytest.mark.parametrize("input,expected", [
    (1, 2),
    (2, 4),
    (3, 6),
    (0, 0),
    (-1, -2),
])
def test_double(input, expected):
    assert double(input) == expected
```

## Test Coverage

Aim for high coverage, but **coverage is not the goal** - confidence is.

Test:
- **Happy path** - Normal, expected inputs
- **Edge cases** - Boundaries, limits
- **Error cases** - Invalid inputs, exceptions
- **Corner cases** - Unusual combinations

## Test Naming

Use descriptive names that explain what's being tested:

```python
# Bad
def test1(): pass
def test_user(): pass

# Good
def test_user_creation_with_valid_data(): pass
def test_email_validation_rejects_invalid_format(): pass
def test_empty_cart_checkout_raises_error(): pass
```

## Avoid Test Logic

Tests should be simple. No loops, conditionals, or complex logic:

```python
# Bad - logic in test
def test_sum_numbers():
    numbers = [1, 2, 3, 4, 5]
    expected = 0
    for n in numbers:
        expected += n
    assert sum_numbers(numbers) == expected

# Good - explicit values
def test_sum_numbers():
    numbers = [1, 2, 3, 4, 5]
    assert sum_numbers(numbers) == 15
```

## Key Takeaways

1. **Clean tests** - Readable, maintainable test code
2. **One concept** - Each test verifies one thing
3. **F.I.R.S.T.** - Fast, Independent, Repeatable, Self-Validating, Timely
4. **Domain language** - Build test helpers
5. **TDD** - Write tests first
6. **Independence** - Tests don't rely on each other
7. **No logic** - Keep tests simple and explicit
