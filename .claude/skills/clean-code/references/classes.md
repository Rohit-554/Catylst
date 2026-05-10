# Classes and Objects

## Class Organization

### Standard Class Layout

```python
# 1. Constants
MAX_USERS = 100

class User:
    # 2. Class variables
    user_count = 0
    
    # 3. Instance variables
    def __init__(self):
        self.name = None        # Public
        self.email = None       # Public
        self._password = None   # Private
    
    # 4. Public methods
    def get_name(self):
        return self.name
    
    # 5. Private methods
    def _validate_password(self):
        pass
```

### Encapsulation

Keep variables and utility functions private. Only expose what's necessary:

```python
# Bad - too much exposed
class User:
    def __init__(self):
        self.raw_password = None
        self.salt = None
        self.hash_iterations = 10000

# Good - encapsulated
class User:
    def __init__(self):
        self._password_hash = None
    
    def set_password(self, password):
        self._password_hash = self._hash_password(password)
    
    def _hash_password(self, password):
        # Private implementation details
        pass
```

## Classes Should Be Small

### First Rule: Small

Classes should be small. Measured by **responsibilities**, not lines of code.

```python
# Bad - too many responsibilities
class User:
    def authenticate(self): pass
    def send_email(self): pass
    def log_activity(self): pass
    def validate_input(self): pass
    def generate_reports(self): pass

# Good - single responsibility
class User:
    def get_name(self): pass
    def set_name(self, name): pass

class Authenticator:
    def authenticate(self, user): pass

class EmailService:
    def send_email(self, user, message): pass
```

### Class Names

A class name should describe its responsibility. If you can't derive a concise name, the class is likely too large.

Avoid words like `Manager`, `Processor`, `Data`, `Info` - they're too vague.

```python
# Vague
class UserManager: pass
class DataProcessor: pass

# Clear
class UserAuthenticator: pass
class OrderValidator: pass
```

## Single Responsibility Principle (SRP)

**A class should have one, and only one, reason to change.**

```python
# Bad - two reasons to change
class Employee:
    def calculate_pay(self): pass  # Finance rules change
    def save(self): pass           # Database structure changes

# Good - separated concerns
class Employee:
    def __init__(self):
        self.name = None
        self.hours_worked = 0

class PayCalculator:
    def calculate_pay(self, employee): pass

class EmployeeRepository:
    def save(self, employee): pass
```

## Cohesion

Classes should have small number of instance variables. Methods should manipulate one or more of those variables.

**High cohesion**: Methods use most of the variables
**Low cohesion**: Methods use few variables

```python
# High cohesion - methods use most instance variables
class Stack:
    def __init__(self):
        self._elements = []
        self._size = 0
    
    def push(self, element):
        self._elements.append(element)
        self._size += 1
    
    def pop(self):
        if self._size == 0:
            raise Exception("Stack empty")
        self._size -= 1
        return self._elements.pop()
```

### Maintaining Cohesion

When classes lose cohesion, split them:

```python
# Bad - losing cohesion
class PrintPrimes:
    def __init__(self):
        self.page_width = 80
        self.primes = []
        self.ordinal_max = 30
    
    def generate_primes(self): pass  # Uses ordinal_max, primes
    def print_page(self): pass       # Uses page_width, primes

# Good - split by responsibility
class PrimeGenerator:
    def __init__(self):
        self.ordinal_max = 30
        self.primes = []
    
    def generate(self): pass

class NumberPrinter:
    def __init__(self):
        self.page_width = 80
    
    def print_page(self, numbers): pass
```

## Open-Closed Principle (OCP)

**Classes should be open for extension, but closed for modification.**

```python
# Bad - must modify class to add new shape
class AreaCalculator:
    def calculate_area(self, shapes):
        total = 0
        for shape in shapes:
            if shape.type == "circle":
                total += math.pi * shape.radius ** 2
            elif shape.type == "square":
                total += shape.side ** 2
        return total

# Good - open for extension
from abc import ABC, abstractmethod

class Shape(ABC):
    @abstractmethod
    def area(self):
        pass

class Circle(Shape):
    def __init__(self, radius):
        self.radius = radius
    
    def area(self):
        return math.pi * self.radius ** 2

class AreaCalculator:
    def calculate_area(self, shapes):
        return sum(shape.area() for shape in shapes)
```

## Dependency Inversion Principle (DIP)

**Depend on abstractions, not concretions.**

```python
# Bad - depends on concrete implementation
class UserService:
    def __init__(self):
        self.notifier = EmailNotifier()  # Concrete dependency
    
    def register_user(self, user):
        self.notifier.send("Welcome!")

# Good - depends on abstraction
class Notifier(ABC):
    @abstractmethod
    def send(self, message):
        pass

class EmailNotifier(Notifier):
    def send(self, message):
        # Email implementation
        pass

class UserService:
    def __init__(self, notifier: Notifier):  # Depends on abstraction
        self.notifier = notifier
    
    def register_user(self, user):
        self.notifier.send("Welcome!")
```

## Organizing for Change

Structure code so changes are isolated:

```python
# Bad - changes ripple
class SQL:
    def create(self, table): pass
    def find_by_id(self, table, id): pass
    def update(self, table, data): pass

# Good - isolated changes
class SQL(ABC):
    @abstractmethod
    def generate(self): pass

class CreateSQL(SQL):
    def __init__(self, table, columns):
        self.table = table
        self.columns = columns
    
    def generate(self):
        return f"CREATE TABLE {self.table} ..."

class SelectSQL(SQL):
    def __init__(self, table, where):
        self.table = table
        self.where = where
    
    def generate(self):
        return f"SELECT * FROM {self.table} WHERE {self.where}"
```

## Law of Demeter

**Talk to friends, not to strangers.**

A method should only call methods on:
- Itself
- Objects passed as arguments
- Objects it creates
- Objects in instance variables

```python
# Bad - train wreck (chaining)
output_dir = ctx.get_options().get_scratch_dir().get_absolute_path()

# Good - tell, don't ask
output_dir = ctx.get_scratch_directory_path()

# Or better - hide the details
ctx.create_scratch_file(file_name)
```

## Key Takeaways

1. **Small classes** - One responsibility per class
2. **High cohesion** - Variables and methods belong together
3. **Low coupling** - Classes don't know too much about each other
4. **Open/Closed** - Open for extension, closed for modification
5. **Dependency Inversion** - Depend on abstractions
6. **Organize for change** - Structure makes changes isolated
