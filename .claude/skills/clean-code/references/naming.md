# Naming Conventions

## Core Principle

Names should reveal intent. A reader should understand what a variable represents, what a function does, or what a class is responsible for without reading implementation details or comments.

## Variables

### Use Intention-Revealing Names

```python
# Bad
d = 7  # days

# Good
elapsed_days = 7
days_since_creation = 7
```

### Avoid Disinformation

Don't use names that vary in small ways:
```python
# Bad
user_list_for_processing
user_list_for_validation

# Good
users_to_process
users_to_validate
```

### Make Meaningful Distinctions

Avoid number series and noise words:
```python
# Bad
data1, data2, info, the_data

# Good
source_data, validated_data, user_profile
```

### Use Pronounceable Names

```python
# Bad
gen_ymdhms  # generation year, month, day, hour, minute, second

# Good
generation_timestamp
```

### Use Searchable Names

Single-letter names and numeric constants are hard to locate:
```python
# Bad
if s == 4:  # what is s? what is 4?

# Good
WORK_DAYS_PER_WEEK = 5
if task_count == WORK_DAYS_PER_WEEK:
```

### Avoid Encodings

Hungarian notation and member prefixes are obsolete:
```python
# Bad
str_name, m_description, i_count

# Good
name, description, count
```

## Class Names

Use nouns or noun phrases:
```python
# Good
Customer, WikiPage, Account, AddressParser

# Avoid
Manager, Processor, Data, Info (too generic)
```

## Function/Method Names

Use verbs or verb phrases:
```python
# Good
delete_page()
save_user()
is_valid()
get_name()
```

## Pick One Word Per Concept

Be consistent across your codebase:
```python
# Bad - pick ONE
fetch_user()
retrieve_account()
get_product()

# Good - consistent
get_user()
get_account()
get_product()
```

## Use Solution Domain Names

Programmers read your code, so use CS terms:
```python
# Good
job_queue  # queue is a well-known data structure
visitor_pattern  # pattern name from design patterns
```

## Use Problem Domain Names

When no programmer-centric name fits, use domain-specific terminology:
```python
# Good (for healthcare)
patient_chart
diagnosis_code
treatment_plan
```

## Add Meaningful Context

```python
# Bad - unclear context
first_name = "John"
last_name = "Doe"
street = "123 Main St"

# Good - clear context
class Address:
    def __init__(self):
        self.first_name = "John"
        self.last_name = "Doe"
        self.street = "123 Main St"
```

## Don't Add Gratuitous Context

```python
# Bad - redundant prefix
class GasStationDeluxe:
    def gas_station_deluxe_fill_up(self): pass

# Good
class GasStationDeluxe:
    def fill_up(self): pass
```

## Boolean Variables

Use predicates that read naturally:
```python
# Good
is_valid
has_permission
can_edit
should_retry
was_successful
```

## Collections

Use plural forms:
```python
# Good
users = []
active_customers = []
pending_orders = []
```

## Constants

Use UPPER_SNAKE_CASE:
```python
# Good
MAX_RETRY_COUNT = 3
DEFAULT_TIMEOUT_SECONDS = 30
API_BASE_URL = "https://api.example.com"
```

## Language-Specific Conventions

### Python
- `snake_case` for functions and variables
- `PascalCase` for classes
- `UPPER_SNAKE_CASE` for constants

### JavaScript/TypeScript
- `camelCase` for functions and variables
- `PascalCase` for classes
- `UPPER_SNAKE_CASE` for constants

### Java
- `camelCase` for methods and variables
- `PascalCase` for classes
- `UPPER_SNAKE_CASE` for constants

### Kotlin
- `camelCase` for functions and variables
- `PascalCase` for classes
- `UPPER_SNAKE_CASE` for constants

### C#
- `PascalCase` for public methods, properties, classes
- `camelCase` for local variables, parameters
- `_camelCase` for private fields (with underscore prefix)

## Anti-Patterns to Avoid

1. **Mental mapping** - `i`, `j`, `k` only acceptable in tiny loops
2. **Cute names** - `whack()` instead of `kill()`, `holyHandGrenade()` instead of `delete_items()`
3. **Puns** - Don't use same word for different purposes (`add` for math vs collection)
4. **Fear of long names** - Prefer clarity over brevity: `calculate_average_monthly_revenue()` over `calc_avg()`
