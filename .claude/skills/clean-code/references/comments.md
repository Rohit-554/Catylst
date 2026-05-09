# Comments

## Core Principle

**The proper use of comments is to compensate for our failure to express ourselves in code.**

Comments lie. Not always, not intentionally, but they do. Code changes and evolves, comments don't always follow.

**Truth can only be found in one place: the code.**

## Good Comments

### Legal Comments

Copyright and authorship statements:
```python
# Copyright (C) 2024 by Company Inc.
# Released under the terms of the GNU General Public License version 2 or later.
```

### Informative Comments

Sometimes useful to provide basic information:
```python
# Returns an instance of the Responder being tested
def responder_instance():
    return Responder()

# Better: use function name
def responder_being_tested():
    return Responder()
```

### Explanation of Intent

Explain the decision or intent:
```python
# We use a priority queue here because we need to process
# high-priority items first. The overhead of maintaining the heap
# is acceptable given our use case of < 1000 items
priority_queue = PriorityQueue()
```

### Clarification

Translate obscure arguments or return values:
```python
self.assertTrue(a.compareTo(b) == -1)  # a < b
self.assertTrue(b.compareTo(a) == 1)   # b > a
```

### Warning of Consequences

```python
# Don't run unless you have a lot of time
def test_with_really_big_file():
    pass

# SimpleDateFormat is not thread-safe,
# so we need to create a new instance each time
def format_date(date):
    formatter = SimpleDateFormat("yyyy-MM-dd")
    return formatter.format(date)
```

### TODO Comments

Notes about things that should be done:
```python
# TODO: This is a temporary workaround. Need to implement proper
# connection pooling in the next sprint
def get_database_connection():
    return create_new_connection()
```

### Public API Documentation

Docstrings for public APIs:
```python
def calculate_compound_interest(principal, rate, time, frequency):
    """
    Calculate compound interest.
    
    Args:
        principal: Initial amount
        rate: Annual interest rate (as decimal, e.g., 0.05 for 5%)
        time: Time period in years
        frequency: Compounding frequency per year
        
    Returns:
        Final amount including interest
    """
    return principal * (1 + rate / frequency) ** (frequency * time)
```

## Bad Comments

### Redundant Comments

Comments that say exactly what code says:
```python
# Bad - completely redundant
# The user's name
name = user.get_name()

# Sets the user's age
def set_age(age):
    self.age = age
```

### Misleading Comments

```python
# Bad - misleading (doesn't close when condition is met)
# Utility method that closes the output stream
def close_when_done():
    if (output_stream.is_open()):
        output_stream.close()
```

### Mandated Comments

Requiring a comment for every function is silly:
```python
# Bad - forced, adds no value
# @param title The title of the CD
# @param author The author of the CD
def add_cd(title, author):
    pass

# Good - self-documenting
class CD:
    def __init__(self, title, author):
        pass
```

### Journal Comments

Version control handles history:
```python
# Bad - use git
# Changes (from 1-Jan-2024)
# 1-Jan-2024: Added error handling (John)
# 5-Jan-2024: Fixed bug #123 (Jane)
```

### Noise Comments

Restating the obvious:
```python
# Bad - pure noise
# Default constructor
def __init__(self):
    pass

# The day of the month
self.day_of_month = 0
```

### Section-Header Comments

Banners and dividers that split a file into regions are a code smell — they signal the class has too many responsibilities:
```python
# Bad - section headers mask a bloated class
# --- Validation ---
def validate_name(self): ...
def validate_email(self): ...

# --- Persistence ---
def save(self): ...
def delete(self): ...

# Good - blank lines between concept groups
def validate_name(self): ...
def validate_email(self): ...

def save(self): ...
def delete(self): ...

# Best - if you need headers, split the class
class UserValidator: ...
class UserRepository: ...
```

### Closing Brace Comments

```python
# Bad - function too long if you need this
def process_data():
    while condition:
        if something:
            for item in items:
                # ... 50 lines of code
            # end for
        # end if
    # end while

# Good - small functions don't need this
```

### Commented-Out Code

**Delete it!** Version control remembers:
```python
# Bad - pollutes the codebase
# old_function()
# legacy_code()

# Good - just delete it
```

### HTML in Comments

Don't use HTML in source code comments:
```python
# Bad
# <p>This function does something</p>
# <ul><li>Step 1</li></ul>
```

### Too Much Information

Don't put interesting historical discussions or irrelevant details:
```python
# Bad - way too much
# RFC 2045 - Multipurpose Internet Mail Extensions (MIME) Part One...
# (300 more lines)
def encode_base64(data):
    pass
```

### Function Headers

Short functions don't need much description. Good name is better:
```python
# Bad
# Processes the user registration by validating input, creating account...
def process_user_registration(user):
    pass

# Good - function name says it all
def validate_and_create_user_account(user):
    pass
```

## The Better Alternative

Instead of writing comments, spend time making code self-explanatory:

```python
# Bad - needs comment
# Check to see if employee is eligible for full benefits
if (employee.flags & HOURLY_FLAG) and (employee.age > 65):
    pass

# Good - expressive code
if employee.is_eligible_for_full_benefits():
    pass
```

## When in Doubt

Ask yourself:
1. Can I express this in code instead?
2. Can I rename to make this clear?
3. Can I extract a method to make this obvious?
4. Is this comment actually helpful, or just noise?

**If you must write a comment, make it count.**
