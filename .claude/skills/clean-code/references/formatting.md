# Formatting

## Why Formatting Matters

Code formatting is about **communication**, and communication is the professional developer's first order of business.

The functionality you create today has a good chance of changing, but the readability of your code will have a profound effect on all changes that follow.

## Vertical Formatting

### File Size

- Small files are easier to understand than large ones
- Most files should be 200-500 lines
- Files over 500 lines should be split

### Newspaper Metaphor

Code should read like a newspaper article:
- Name should be simple but explanatory
- Topmost parts give high-level concepts
- Details increase as you move downward

### Vertical Openness

Use blank lines to separate concepts:

```python
# Bad - no separation
def calculate_total(items):
    total = 0
    for item in items:
        total += item.price
    tax = total * 0.1
    return total + tax

# Good - logical grouping
def calculate_total(items):
    total = 0
    
    for item in items:
        total += item.price
    
    tax = total * 0.1
    return total + tax
```

### Vertical Density

Lines closely related should appear vertically dense:

```python
# Bad - unnecessary blank lines
class User:

    def __init__(self):
    
        self.name = None
        
        self.email = None

# Good - related lines together
class User:
    def __init__(self):
        self.name = None
        self.email = None
```

### Vertical Distance

Concepts closely related should be kept vertically close. Declare variables as close to usage as possible:

```python
# Bad - declaration far from usage
def process_data():
    result = None  # Declared here
    
    # 50 lines of other code
    
    result = compute()  # Used here
    return result

# Good - declaration near usage
def process_data():
    # ... other code
    
    result = compute()
    return result
```

### Dependent Functions

If one function calls another, keep them vertically close, with caller above callee:

```python
# Good - caller above callee
def process_user_registration(user):
    validate_user(user)
    save_to_database(user)
    send_welcome_email(user)

def validate_user(user):
    check_email_format(user.email)
    check_age_requirement(user.age)

def check_email_format(email):
    # Implementation
    pass
```

### Vertical Ordering

Functions should be called in a downward direction (stepdown rule):

```python
# Good - high-level to low-level
def render_page():
    build_header()
    build_content()
    build_footer()

def build_header():
    add_logo()
    add_navigation()

def add_logo():
    # Implementation
    pass
```

## Horizontal Formatting

### Line Length

- Keep lines short (80-120 characters max)
- 80 characters is traditional and safe
- 120 is reasonable for modern screens

```python
# Bad - too long
def create_user_with_profile(first_name, last_name, email, phone, address, city, state, zip_code, country, date_of_birth):
    pass

# Good - wrapped appropriately
def create_user_with_profile(
    first_name, last_name, email, phone,
    address, city, state, zip_code,
    country, date_of_birth
):
    pass
```

### Horizontal Openness and Density

Use whitespace to associate related things and disassociate unrelated:

```python
# Bad - poor spacing
def quadratic(a,b,c):
    discriminant=b*b-4*a*c
    root1=(-b+math.sqrt(discriminant))/(2*a)

# Good - proper spacing
def quadratic(a, b, c):
    discriminant = b*b - 4*a*c
    root1 = (-b + math.sqrt(discriminant)) / (2*a)
```

Spacing guidelines:
- Space after commas: `func(a, b, c)`
- Space around operators: `x = y + z`
- No space in precedence: `b*b - 4*a*c`
- Space after keywords: `if (condition)`

### Indentation

Code is a hierarchy - use indentation to show it:

```python
# Bad - no indentation
def process_orders():
for order in orders:
if order.is_valid():
process_payment(order)

# Good - proper indentation
def process_orders():
    for order in orders:
        if order.is_valid():
            process_payment(order)
```

### Don't Collapse Scopes

```python
# Bad - collapsed
class User: pass
def get_name(self): return self.name

# Good - proper structure
class User:
    pass

def get_name(self):
    return self.name
```

## Team Rules

**A team should agree upon a single formatting style.**

Once agreed:
- Every team member uses that style
- Automated formatters help enforce consistency
- Don't let formatting debates waste time

### Language-Specific Formatters

Use automated formatters:
- **Python**: Black, autopep8
- **JavaScript**: Prettier, ESLint
- **Java**: Google Java Format, IntelliJ formatter
- **Kotlin**: ktlint
- **C#**: StyleCop, ReSharper
- **Go**: gofmt (built-in)
- **Rust**: rustfmt

## Key Takeaways

1. **Be consistent** - Whatever rules you choose, apply them uniformly
2. **Use tools** - Automated formatters prevent debates
3. **Prioritize readability** - Format to make code easy to read
4. **Follow language idioms** - Respect the conventions of your language
5. **Team agreement** - One style for the whole team
