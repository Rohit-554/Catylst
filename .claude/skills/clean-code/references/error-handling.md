# Error Handling

## Core Principles

1. **Use exceptions**, not return codes
2. **Provide context** with exceptions
3. **Don't return null** - use Optional/Maybe patterns
4. **Don't pass null** as arguments
5. **Fail fast** - detect problems early

Error handling is important, but if it obscures logic, it's doing it wrong.

## Use Exceptions Rather Than Return Codes

Return codes clutter the caller and require immediate handling:

```python
# Bad - error codes
def delete_page(page):
    status = delete_page_and_all_references(page)
    if status == E_OK:
        registry_status = registry.delete_reference(page.name)
        if registry_status == E_OK:
            return E_OK
        else:
            return E_ERROR
    else:
        return E_ERROR

# Good - exceptions
def delete_page(page):
    try:
        delete_page_and_all_references(page)
        registry.delete_reference(page.name)
    except Exception as e:
        logger.log(str(e))
        raise DeletionError(f"Failed to delete page {page.name}") from e
```

## Write Try-Catch-Finally First

Exceptions define a scope. Write tests that force exceptions, then build handler:

```python
# 1. Start with test that forces exception
def test_retrieve_section_should_throw_on_invalid_file():
    with pytest.raises(StorageException):
        retriever = PageDataRetriever()
        retriever.retrieve_section("invalid_file")

# 2. Implement with try-catch structure
def retrieve_section(self, page_name):
    try:
        with open(page_name, 'r') as file:
            return file.read()
    except FileNotFoundError:
        raise StorageException("Section not found")
```

## Provide Context with Exceptions

Create informative error messages with context:

```python
# Bad - no context
raise Exception("Invalid input")

# Good - specific exception with context
class ValidationError(Exception):
    pass

def validate_user_age(age):
    if age < 0:
        raise ValidationError(
            f"Age cannot be negative. Received: {age}"
        )
    if age > 150:
        raise ValidationError(
            f"Age {age} is unrealistic. Please verify input."
        )
```

## Define Exception Classes by Caller's Needs

Classify exceptions by how they'll be caught and handled:

```python
# Bad - too many exception types for caller
def call_external_api():
    try:
        return api.get_data()
    except ConnectionError:
        # Handle connection error
    except TimeoutError:
        # Handle timeout
    except AuthenticationError:
        # Handle auth error
    # All handled the same way: log and retry

# Good - wrap in single exception type
class APIException(Exception):
    def __init__(self, message, cause=None):
        super().__init__(message)
        self.cause = cause

def call_external_api():
    try:
        return api.get_data()
    except (ConnectionError, TimeoutError, AuthenticationError) as e:
        raise APIException("API call failed", cause=e)

# Caller handles one exception type
def process_data():
    try:
        data = call_external_api()
        return process(data)
    except APIException as e:
        logger.error(f"Failed to fetch data: {e}")
        return default_data
```

## Don't Return Null

Returning null creates work for every caller:

```python
# Bad - returns null
def get_user(user_id):
    user = database.find_user(user_id)
    if user:
        return user
    return None

# Every caller must check
user = get_user(123)
if user is not None:  # Easy to forget!
    process_user(user)

# Good - raise exception
def get_user(user_id):
    user = database.find_user(user_id)
    if not user:
        raise UserNotFoundError(f"User {user_id} not found")
    return user

# Or return Optional
from typing import Optional

def find_user(user_id) -> Optional[User]:
    return database.find_user(user_id)

# Caller forced to handle absence
user = find_user(123)
if user:
    process_user(user)
```

### Special Case Pattern

Instead of null, return a special case object:

```python
# Bad - returns null
def get_employee(id):
    employee = db.find(id)
    if employee:
        return employee
    return None

# Good - Special Case Pattern
class Employee:
    def calculate_pay(self):
        return self.salary * self.bonus_multiplier

class NullEmployee(Employee):
    def calculate_pay(self):
        return 0

def get_employee(id):
    employee = db.find(id)
    if employee:
        return employee
    return NullEmployee()  # Never returns None

# No null checks needed
emp = get_employee(123)
pay = emp.calculate_pay()  # Works even if employee not found
```

## Validation Functions — Never Return Nullable Strings

A common anti-pattern: validation functions that return `null` for success and an error string for failure. This violates "never return null" and forces callers into null-checking:

```kotlin
// Bad — nullable String encodes two meanings in one type
fun validateWithdrawalAmount(amount: BigDecimal): String? {
    if (amount <= BigDecimal.ZERO) return "Amount must be positive"
    if (amount > balance) return "Insufficient funds"
    return null  // null means "valid" — invisible contract
}

// Caller must remember what null means
val error = validateWithdrawalAmount(amount)
if (error != null) { showError(error) }

// Good — sealed type makes success/failure explicit
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

fun validateWithdrawalAmount(amount: BigDecimal): ValidationResult {
    if (amount <= BigDecimal.ZERO) return Invalid("Amount must be positive")
    if (amount > balance) return Invalid("Insufficient funds")
    return Valid
}

// Caller is forced to handle both cases
when (val result = validateWithdrawalAmount(amount)) {
    is Valid -> processWithdrawal(amount)
    is Invalid -> showError(result.reason)
}

// Also good — throw if validation is a precondition
fun requireValidWithdrawalAmount(amount: BigDecimal) {
    require(amount > BigDecimal.ZERO) { "Amount must be positive" }
    require(amount <= balance) { "Insufficient funds: $amount exceeds $balance" }
}
```

Choose based on context:
- **Sealed Result**: when the caller needs to handle both paths (form validation, UI feedback)
- **Throw**: when invalid input is a programming error or precondition violation

## Don't Pass Null

Passing null into methods is even worse than returning it:

```python
# Bad - accepts null
def calculate_total(items):
    if items is None:
        return 0
    total = 0
    for item in items:
        if item is not None:
            total += item.price
    return total

# Good - require non-null
def calculate_total(items):
    """Calculate total price of items.
    
    Args:
        items: List of items (must not be None)
    """
    if not isinstance(items, list):
        raise TypeError("items must be a list")
    
    return sum(item.price for item in items)
```

## Separate Business Logic from Error Handling

Extract try/catch blocks:

```python
# Bad - mixed logic and error handling
def process_order(order):
    try:
        if not order.items:
            raise ValueError("Empty order")
        total = sum(item.price for item in order.items)
        payment_result = payment_processor.charge(total)
        if not payment_result.success:
            raise PaymentError()
        shipping.schedule(order)
    except ValueError as e:
        logger.error(f"Invalid order: {e}")
        return False
    except PaymentError:
        logger.error("Payment failed")
        return False
    return True

# Good - separated concerns
def process_order(order):
    try:
        execute_order(order)
        return True
    except OrderError as e:
        handle_order_error(order, e)
        return False

def execute_order(order):
    validate_order(order)
    total = calculate_order_total(order)
    process_payment(order, total)
    schedule_shipping(order)
```

## Fail Fast

Detect problems early:

```python
# Bad - fails late
def process_batch(items):
    results = []
    for item in items:
        try:
            result = process_item(item)
            results.append(result)
        except Exception:
            pass  # Silently ignore errors
    return results

# Good - fail fast
def process_batch(items):
    if not items:
        raise ValueError("Cannot process empty batch")
    
    validate_all_items(items)  # Validate before processing
    
    results = []
    for item in items:
        result = process_item(item)
        results.append(result)
    return results
```

## Key Takeaways

1. **Use exceptions** - Don't return error codes
2. **Provide context** - Include helpful error messages
3. **Never return null** - Use exceptions or Optional
4. **Never pass null** - Validate inputs
5. **Extract error handling** - Separate from business logic
6. **Fail fast** - Detect errors early
7. **Define exception hierarchy** - Group by how they're handled
