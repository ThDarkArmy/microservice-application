# Design Patterns with Java

A practical catalog of object-oriented and enterprise design patterns. A pattern is a named, reusable approach to a recurring design problem, not a rule to apply everywhere.

## 1. How to choose a pattern

1. Describe the problem without naming a pattern.
2. Identify what changes and what should remain stable.
3. Prefer the smallest design that protects that variation.
4. Check readability, testing, lifecycle, and concurrency.
5. Explain why a simpler conditional or composition is insufficient.

Patterns add indirection. Use one when it reduces change cost or clarifies collaboration.

## 2. Pattern families

| Family              | Purpose                         | Examples                                            |
| ------------------- | ------------------------------- | --------------------------------------------------- |
| Creational          | Control object creation         | Factory, Builder, Singleton                         |
| Structural          | Compose objects and interfaces  | Adapter, Decorator, Facade, Proxy                   |
| Behavioral          | Coordinate behavior             | Strategy, Observer, Command, State, Template Method |
| Enterprise          | Organize application boundaries | Repository, Service Layer, DTO, Specification       |
| Distributed systems | Coordinate across services      | Saga, Circuit Breaker, Outbox                       |

## 3. Factory Method

**Problem:** Object creation varies by type, and callers should not depend on concrete classes.

```java
public interface Parser {
    Document parse(String input);
}

public final class JsonParser implements Parser {
    @Override
    public Document parse(String input) {
        return Document.fromJson(input);
    }
}

public final class ParserFactory {
    public static Parser forFormat(String format) {
        return switch (format.toLowerCase()) {
            case "json" -> new JsonParser();
            case "xml" -> new XmlParser();
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
    }
}
```

**Use when:** Creation depends on input or a stable family of implementations.

**Avoid when:** A constructor is clear and there is only one implementation.

## 4. Abstract Factory

**Problem:** Create related objects that must work together without naming concrete classes.

```java
public interface UiFactory {
    Button createButton();
    Dialog createDialog();
}

public final class DarkUiFactory implements UiFactory {
    public Button createButton() { return new DarkButton(); }
    public Dialog createDialog() { return new DarkDialog(); }
}
```

**Use when:** A product family varies together, such as theme-specific UI components or cloud-specific adapters.

## 5. Builder

**Problem:** A constructor has many optional values or creating a valid object requires a sequence of choices.

```java
public final class HttpRequest {
    private final URI uri;
    private final String method;
    private final Map<String, String> headers;
    private final Duration timeout;

    private HttpRequest(Builder builder) {
        this.uri = Objects.requireNonNull(builder.uri);
        this.method = builder.method;
        this.headers = Map.copyOf(builder.headers);
        this.timeout = builder.timeout;
    }

    public static Builder builder(URI uri) {
        return new Builder(uri);
    }

    public static final class Builder {
        private final URI uri;
        private String method = "GET";
        private final Map<String, String> headers = new HashMap<>();
        private Duration timeout = Duration.ofSeconds(5);

        private Builder(URI uri) { this.uri = uri; }
        public Builder method(String method) { this.method = method; return this; }
        public Builder header(String name, String value) {
            headers.put(name, value);
            return this;
        }
        public Builder timeout(Duration timeout) { this.timeout = timeout; return this; }
        public HttpRequest build() { return new HttpRequest(this); }
    }
}
```

**Use when:** You need readable construction of immutable objects with validation.

**Avoid when:** The object has only a few required fields.

## 6. Singleton

**Problem:** A process needs one shared instance, such as a configuration registry.

```java
public enum ApplicationClock {
    INSTANCE;

    public Instant now() {
        return Instant.now();
    }
}
```

**Use carefully:** A singleton is global mutable state if it stores data. Prefer dependency injection and a framework-managed singleton bean in Spring. Inject `Clock` for testable time instead of calling global time APIs throughout business code.

## 7. Adapter

**Problem:** Make an incompatible external interface fit the interface your application expects.

```java
public interface PaymentGateway {
    PaymentResult charge(Money amount, String token);
}

public final class StripePaymentAdapter implements PaymentGateway {
    private final StripeClient client;

    public StripePaymentAdapter(StripeClient client) {
        this.client = client;
    }

    @Override
    public PaymentResult charge(Money amount, String token) {
        StripeCharge charge = client.createCharge(amount.amount(), amount.currency(), token);
        return new PaymentResult(charge.id(), charge.succeeded());
    }
}
```

**Use when:** Integrating payment providers, SDKs, legacy APIs, or third-party clients.

## 8. Decorator

**Problem:** Add behavior to an object without changing its class or creating many subclasses.

```java
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    void save(Order order);
}

public final class CachingOrderRepository implements OrderRepository {
    private final OrderRepository delegate;
    private final Map<OrderId, Order> cache = new ConcurrentHashMap<>();

    public CachingOrderRepository(OrderRepository delegate) {
        this.delegate = delegate;
    }

    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(cache.computeIfAbsent(id,
                key -> delegate.findById(key).orElse(null)));
    }

    public void save(Order order) {
        delegate.save(order);
        cache.put(order.id(), order);
    }
}
```

Other decorators can add metrics, logging, authorization, retries, or transactions.

## 9. Facade

**Problem:** Provide a simple entry point over a complicated subsystem.

```java
public final class CheckoutFacade {
    private final InventoryService inventory;
    private final PaymentGateway payments;
    private final OrderRepository orders;

    public CheckoutResult checkout(CheckoutRequest request) {
        inventory.reserve(request.items());
        payments.charge(request.total(), request.paymentToken());
        Order order = orders.save(Order.create(request));
        return new CheckoutResult(order.id());
    }
}
```

**Use when:** A use case coordinates several internal services. Keep the facade from becoming a god class by delegating domain rules to the right objects.

## 10. Proxy

**Problem:** Control access to another object or add behavior around it.

Examples include lazy loading, access checks, remote clients, caching, and Spring AOP proxies.

```java
public final class AuthorizedPaymentGateway implements PaymentGateway {
    private final PaymentGateway delegate;
    private final AuthorizationService authorization;

    public PaymentResult charge(Money amount, String token) {
        authorization.requirePermission("payments:charge");
        return delegate.charge(amount, token);
    }
}
```

## 11. Strategy

**Problem:** A family of algorithms varies independently from the code that uses it.

```java
public interface PricingStrategy {
    Money calculate(Order order);
}

public final class RegularPricing implements PricingStrategy {
    public Money calculate(Order order) { return order.subtotal(); }
}

public final class VipPricing implements PricingStrategy {
    public Money calculate(Order order) {
        return order.subtotal().multiply(new BigDecimal("0.90"));
    }
}

public final class PriceCalculator {
    private final PricingStrategy strategy;

    public PriceCalculator(PricingStrategy strategy) {
        this.strategy = strategy;
    }

    public Money calculate(Order order) {
        return strategy.calculate(order);
    }
}
```

**Use when:** Rules are selected by customer type, payment method, shipping option, or feature configuration.

## 12. Observer and domain events

**Problem:** Notify multiple listeners when something happens without coupling the publisher to every listener.

```java
public record OrderPlaced(OrderId orderId, Instant occurredAt) {
}

public interface DomainEventHandler<T> {
    void handle(T event);
}
```

In Spring, application events can be useful inside one process. For cross-service delivery, use a durable broker and an outbox rather than an in-memory observer.

## 13. Command

**Problem:** Represent an action as an object so it can be queued, logged, retried, authorized, or undone.

```java
public interface Command {
    void execute();
}

public final class ReserveStockCommand implements Command {
    private final Inventory inventory;
    private final SKU sku;
    private final int quantity;

    public void execute() {
        inventory.reserve(sku, quantity);
    }
}
```

Commands are useful for job queues, audit trails, workflow steps, and CQRS command handlers.

## 14. State

**Problem:** An object's behavior changes when its internal state changes.

```java
public interface OrderState {
    OrderState pay(Order order);
    OrderState ship(Order order);
}

public final class CreatedState implements OrderState {
    public OrderState pay(Order order) {
        return new PaidState();
    }

    public OrderState ship(Order order) {
        throw new IllegalStateException("An unpaid order cannot ship");
    }
}
```

For a small state machine, an enum and transition table may be clearer. Use state objects when each state has substantial behavior.

## 15. Template Method

**Problem:** The workflow is fixed, but selected steps vary.

```java
public abstract class ReportExporter {
    public final void export(Report report) {
        String data = load(report);
        String formatted = format(data);
        write(formatted);
    }

    protected abstract String load(Report report);
    protected abstract String format(String data);
    protected abstract void write(String data);
}
```

Prefer composition and Strategy when inheritance would create a rigid hierarchy.

## 16. Repository and Service Layer

These are common enterprise patterns rather than GoF patterns.

### Repository

Abstract persistence operations around an aggregate or entity.

```java
public interface ProductRepository {
    Optional<Product> findById(ProductId id);
    Product save(Product product);
}
```

### Service Layer

Coordinates an application use case and transaction boundary.

```java
@Service
public class ProductApplicationService {
    private final ProductRepository repository;

    @Transactional
    public ProductView rename(ProductId id, String name) {
        Product product = repository.findById(id).orElseThrow();
        product.rename(name);
        return ProductView.from(repository.save(product));
    }
}
```

Keep domain behavior in domain objects when possible; do not turn service classes into collections of unrelated procedures.

## 17. DTO and Mapper

**Problem:** Prevent persistence or domain models from becoming accidental public API contracts.

```java
public record ProductResponse(UUID id, String name, BigDecimal price) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.id().value(), product.name(), product.price());
    }
}
```

Use request and response DTOs at API boundaries. Map explicitly when security, compatibility, or differing shapes matter.

## 18. Specification

**Problem:** Compose reusable business predicates.

```java
public interface Specification<T> {
    boolean isSatisfiedBy(T candidate);

    default Specification<T> and(Specification<T> other) {
        return candidate -> isSatisfiedBy(candidate) && other.isSatisfiedBy(candidate);
    }
}

Specification<Product> available = product -> product.stock() > 0;
Specification<Product> affordable = product -> product.price().compareTo(limit) <= 0;
Specification<Product> wanted = available.and(affordable);
```

Use it for composable filtering rules. Avoid it when a direct query or one readable predicate is clearer.

## 19. Distributed patterns

### Circuit breaker

Wrap calls to failing dependencies and stop sending traffic during an outage. Libraries such as Resilience4j provide production implementations.

### Retry with backoff

Retry transient failures with bounded attempts, exponential delay, and jitter. Pair retries with idempotency.

### Saga

Coordinate local transactions across services with events and compensating actions.

### Transactional outbox

Write a business change and its outgoing event in one database transaction, then publish the event asynchronously.

### Strangler Fig

Gradually replace a legacy system by routing selected capabilities to new services while the old system remains operational.

## 20. Spring Boot pattern mapping

| Need                   | Typical Spring or Java approach                                    |
| ---------------------- | ------------------------------------------------------------------ |
| Dependency injection   | Constructor injection with `@Service`, `@Repository`, `@Component` |
| REST boundary          | `@RestController` and DTOs                                         |
| Persistence boundary   | Spring Data repository plus domain model                           |
| Cross-cutting behavior | Decorator, Proxy, filters, or AOP                                  |
| External integration   | Adapter around a client SDK                                        |
| Configurable algorithm | Strategy with injected implementations                             |
| Events inside one app  | Application events and listeners                                   |
| Resilience             | Resilience4j circuit breaker, retry, timeout, bulkhead             |
| Transactions           | `@Transactional` at an application boundary                        |

## 21. Anti-patterns to recognize

- **God object:** One class owns unrelated data and behavior.
- **Deep inheritance:** Changes ripple through a fragile hierarchy.
- **Singleton everywhere:** Hidden global state and difficult tests.
- **Anemic domain model:** Objects hold data while all rules live in procedural services.
- **Pattern by name:** Extra factories, interfaces, or handlers without real variation.
- **Distributed monolith:** Services are separated physically but tightly coupled in deployment and database access.
- **Shared mutable DTO:** Internal objects leak into public API contracts.

## 22. Pattern selection checklist

- What behavior or dependency varies?
- Is the variation likely to remain?
- Can composition solve it more simply?
- Where should the abstraction live?
- How will it be tested?
- What lifecycle and thread-safety rules apply?
- Does the pattern clarify the code for the next developer?

## 23. Practice exercises

1. Replace a payment `if/else` chain with Strategy and Adapter.
2. Add a caching Decorator around a repository.
3. Model order lifecycle with State or an explicit transition table.
4. Build a notification Facade over email, SMS, and push adapters.
5. Add an outbox publisher and idempotent event consumer.
6. Review the result and remove any pattern that did not reduce complexity.

For object modeling practice, see [LLD-JAVA.md](LLD-JAVA.md). For service boundaries and distributed patterns, see [HLD-JAVA.md](HLD-JAVA.md).
