# FRONTEND.md — Angular & TypeScript Architecture and Coding Guidelines

## 1. Architecture Overview

The frontend follows a feature-based folder structure. Every concern has a designated place — no logic scattered across random files.

```
src/app/
├── app.component.ts          # Root shell (navigation, layout only)
├── app.config.ts             # ApplicationConfig: providers, router setup
├── app.routes.ts             # Top-level route definitions
├── models/
│   └── models.ts             # All shared TypeScript interfaces and types
├── services/
│   ├── order.service.ts      # HTTP calls for the order domain
│   └── shipment.service.ts   # HTTP calls for the shipment domain
├── components/               # Shared, reusable presentational components
│   └── status-pipeline/
└── pages/                    # Lazy-loaded route-level smart components
    ├── orders-list/
    ├── order-detail/
    ├── shipments-list/
    └── shipment-detail/
```

**Rules:**
- `pages/` — smart (container) components. They fetch data, hold state, and call services.
- `components/` — dumb (presentational) components. They receive data via inputs, emit events via outputs, and never call services directly.
- `services/` — one service per domain concept.
- `models/models.ts` — single source of truth for all shared TypeScript types.

---

## 2. Components

All components are **standalone**. There are no `NgModule` declarations anywhere in the project.

```typescript
@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.css'
})
export class OrderDetailComponent implements OnInit {

  private orderService = inject(OrderService);

  id = input.required<string>();   // bound from route param via withComponentInputBinding()

  order: Order | null = null;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.orderService.getOrder(this.id()).subscribe({
      next: (order) => (this.order = order),
      error: (err) => (this.errorMessage = (err.error as ApiError).message)
    });
  }
}
```

**Rules:**
- Always set `standalone: true` — never add a component to an `NgModule`.
- Use the `inject()` function for dependency injection — never constructor injection.
- Use `input<T>()` and `input.required<T>()` (signal-based) for component inputs.
- Use `output<T>()` for event emitting — replace `@Output() EventEmitter` with the signal API.
- Dumb/presentational components must never call services. They only accept `input()` and emit `output()`.
- Smart/page components fetch data in `ngOnInit()` and pass it down to presentational children.
- Keep templates lean — move complex logic to the component class or a service.
- Use Angular's built-in control flow syntax (`@if`, `@for`, `@switch`) — not `*ngIf` / `*ngFor` directives.

```html
<!-- Good -->
@for (item of items(); track item.id) {
  <app-order-card [order]="item" />
}

<!-- Avoid -->
<app-order-card *ngFor="let item of items" [order]="item" />
```

---

## 3. Routing

All page components are lazy-loaded. The router never eagerly imports page components.

```typescript
// app.routes.ts
export const routes: Routes = [
  { path: '', redirectTo: 'orders', pathMatch: 'full' },
  {
    path: 'orders',
    loadComponent: () =>
      import('./pages/orders-list/orders-list.component')
        .then(m => m.OrdersListComponent)
  },
  {
    path: 'orders/:id',
    loadComponent: () =>
      import('./pages/order-detail/order-detail.component')
        .then(m => m.OrderDetailComponent)
  },
  { path: '**', redirectTo: 'orders' }
];
```

```typescript
// app.config.ts
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient()
  ]
};
```

**Rules:**
- All page components use `loadComponent` — never import them directly in route definitions.
- Enable `withComponentInputBinding()` in `provideRouter()` so route params are automatically bound to `input()` signals.
- Always define a wildcard `**` redirect for unknown routes.
- Use `provideHttpClient()` — never import `HttpClientModule`.

---

## 4. Services

Services handle all HTTP communication. They are stateless data-fetching adapters by default.

```typescript
@Injectable({ providedIn: 'root' })
export class OrderService {

  private http = inject(HttpClient);
  private baseUrl = '/api/v1/orders';

  getAllOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(this.baseUrl);
  }

  getOrder(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.baseUrl}/${id}`);
  }

  createOrder(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.baseUrl, request);
  }
}
```

**Rules:**
- Use `providedIn: 'root'` for all services — tree-shakeable, no need to add to providers arrays.
- Use `inject(HttpClient)` inside the service — not constructor injection.
- HTTP methods always return `Observable<T>` — **never subscribe inside a service**.
- Use relative URLs (e.g. `/api/v1/orders`) — never hardcode `localhost` or a full host. The dev proxy handles host resolution.
- One service per domain concept — do not add unrelated methods to an existing service.
- If the same HTTP call is used by multiple components, it lives in the service — never duplicated across components.

---

## 5. State Management

For simple applications, component-local state with direct service calls is sufficient.

```typescript
// Component-local state — appropriate for small/medium scope
export class ShipmentsListComponent implements OnInit {
  private shipmentService = inject(ShipmentService);

  shipments: ShipmentSummary[] = [];
  loading = false;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loading = true;
    this.shipmentService.search().subscribe({
      next: (data) => {
        this.shipments = data;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = (err.error as ApiError).message;
        this.loading = false;
      }
    });
  }
}
```

**When to introduce a signals store or NgRx:**
- The same data is shared between multiple unrelated components.
- State mutations in one component must reactively update another.
- The application has complex, interdependent async flows.
- You need time-travel debugging or strict action logging.

**Signals for derived/reactive state:**

```typescript
export class ShipmentDetailComponent {
  shipment = signal<Shipment | null>(null);

  canPack = computed(() =>
    this.shipment()?.status === 'CREATED' &&
    (this.shipment()?.packages?.length ?? 0) > 0
  );

  canShip = computed(() =>
    this.shipment()?.status === 'PACKED' &&
    this.shipment()!.packages.every(p => !!p.trackingCode)
  );
}
```

**Rules:**
- Prefer `signal()` and `computed()` over manual boolean flags recalculated in multiple places.
- Use `toSignal()` to convert an `Observable` to a signal when you need reactive template binding.
- Do not introduce NgRx or a signals store until the shared-state threshold above is reached — avoid premature complexity.

---

## 6. TypeScript & Models

All shared interfaces live in one file. TypeScript strict mode is always on.

```typescript
// models/models.ts

export interface Address {
  readonly street: string;
  readonly zipCode: string;
  readonly city: string;
  readonly country: string;
}

export interface Order {
  readonly id: string;
  readonly externalOrderNumber: string;
  readonly deliveryAddress: Address;
  readonly status: OrderStatus;
  readonly positions: OrderPosition[];
  readonly shipment: ShipmentSummary | null;
  readonly createdAt: string;
}

export type OrderStatus = 'CREATED';
export type ShipmentStatus = 'CREATED' | 'PACKED' | 'SHIPPED';

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}
```

**Rules:**
- All shared interfaces and types live in `models/models.ts` — never define types inline in components or services.
- Use union types for status strings (`'CREATED' | 'PACKED' | 'SHIPPED'`) — not plain `string`.
- Mark interface fields as `readonly` when the component should not mutate them.
- `strict: true` must be enabled in `tsconfig.json`. This enforces `strictNullChecks`, `noImplicitAny`, and more.
- Never use `any`. Use `unknown` when the type is genuinely unknown, then narrow it explicitly.
- Avoid non-null assertions (`!`) unless you have proven the value cannot be null at that point. Prefer optional chaining (`?.`) and nullish coalescing (`??`).
- Mirror backend DTOs exactly in the models file. When the backend adds a new field, update `models.ts` first.

```typescript
// Bad
const name: any = getUser().name;

// Good
const name: string = getUser().name ?? 'Unknown';
```

---

## 7. Reactive Forms

Use Reactive Forms for all non-trivial forms. Template-driven forms are acceptable only for single-field, low-stakes inputs.

```typescript
export class CreateOrderComponent {
  private fb = inject(FormBuilder);

  form = this.fb.group({
    externalOrderNumber: ['', [Validators.required]],
    deliveryAddress: this.fb.group({
      street:  ['', Validators.required],
      zipCode: ['', Validators.required],
      city:    ['', Validators.required],
      country: ['', Validators.required]
    }),
    positions: this.fb.array([this.createPosition()])
  });

  get positions(): FormArray {
    return this.form.get('positions') as FormArray;
  }

  createPosition(): FormGroup {
    return this.fb.group({
      sku:         ['', Validators.required],
      quantity:    [1,  [Validators.required, Validators.min(1)]],
      description: ['']
    });
  }

  addPosition(): void {
    this.positions.push(this.createPosition());
  }

  removePosition(index: number): void {
    this.positions.removeAt(index);
  }
}
```

**Rules:**
- Use `FormBuilder` (`inject(FormBuilder)`) for all form construction.
- Use `FormArray` for dynamic lists of form groups (e.g. order positions).
- All validation is declared at the form definition — never manually validate values in component methods.
- Use typed form controls where possible (`FormControl<string>`) to avoid runtime type surprises.
- Disable the submit button by binding to `form.invalid` — never check validity manually on submit.
- Extract `FormGroup` factory methods (e.g. `createPosition()`) to keep the form definition readable.
- Never mutate form values directly — use `patchValue()` or `setValue()`.

---

## 8. HTTP & Error Handling

All HTTP communication goes through Angular's `HttpClient`. Errors are always handled — never silently ignored.

```typescript
// In a component
this.shipmentService.pack(this.shipmentId).subscribe({
  next: (updated) => {
    this.shipment = updated;
    this.successMessage = 'Shipment packed successfully.';
  },
  error: (err) => {
    this.errorMessage = (err.error as ApiError).message;
  }
});
```

**Rules:**
- Always provide an `error` handler in `subscribe({ next, error })` — never subscribe with a single callback and ignore errors.
- Cast `err.error as ApiError` to access the structured error envelope from the backend.
- Display `apiError.message` in the template — never expose raw HTTP status codes or stack traces to the user.
- Unsubscribe from long-lived observables to prevent memory leaks. Use `takeUntilDestroyed()` (Angular 16+):

```typescript
export class MyComponent {
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.service.getLiveData()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (data) => ... });
  }
}
```

- For one-shot HTTP calls (`GET`, `POST` on user action) that complete automatically, `takeUntilDestroyed` is not required.
- At scale, consider an `HttpInterceptor` for global error handling, auth token injection, and loading state — rather than repeating error-handling logic in every component.

---

## 9. SOLID Principles

### S — Single Responsibility

Every class has one reason to change.

- Each service handles one domain concept (`OrderService` → orders only, `ShipmentService` → shipments only).
- Each component has one UI responsibility — a page component that renders a list should not also render a detail view.
- Do not add utility HTTP methods to a component — move them to a service.
- Presentational components only render — they do not fetch, transform, or store data.

### O — Open/Closed

Open for extension, closed for modification.

- Add new features by creating new components and services — do not modify existing, working components to add unrelated behavior.
- Use `@Input()` / `input()` to make presentational components reusable across different contexts without modifying them.
- Use composition — wrap or extend a component via a parent, rather than adding conditional logic inside the child.

### L — Liskov Substitution

Components and services must honor their stated contract.

- A component that accepts an `input()` of type `Order` must work correctly for **any** valid `Order` — never special-case specific IDs or statuses inside a presentational component.
- If a service method returns `Observable<Order[]>`, it must always emit an array — never `null` or `undefined`.

### I — Interface Segregation

Keep service APIs small and focused.

- Do not add unrelated methods to an existing service. If `OrderService` starts accumulating shipment-related calls, extract a `ShipmentService`.
- Components should only inject services they actually use. If a component only calls one method, consider whether it needs the whole service or a more targeted abstraction.

### D — Dependency Inversion

Depend on abstractions; make dependencies explicit and swappable.

- Components depend on services via `inject()` — never instantiate services with `new`.
- For testability or swappable implementations, use Angular's `InjectionToken`:

```typescript
export const ORDER_SERVICE = new InjectionToken<OrderServiceInterface>('OrderService');

// In tests or alternative configs, provide a mock:
{ provide: ORDER_SERVICE, useClass: MockOrderService }
```

---

## 10. DRY Principles

Every piece of logic, markup, or type definition has a single home.

**Shared UI → reusable component:**
```html
<!-- Bad: copy-pasted status badge in 3 different templates -->
<span class="status">{{ shipment.status }}</span>

<!-- Good: extracted reusable component -->
<app-status-badge [status]="shipment.status" />
```

**Shared logic → service or utility function:**
```typescript
// Bad: same date formatting duplicated in 4 components
formatDate(iso: string): string { return new Date(iso).toLocaleDateString(); }

// Good: one utility function
// utils/format.ts
export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString();
}
```

**Shared types → `models/models.ts`:**
```typescript
// Bad: interface redefined in a component file
interface LocalOrder { id: string; status: string; }

// Good: import from the single source of truth
import { Order } from '../../models/models';
```

**Rules:**
- If the same template fragment appears in more than one component, extract it into a shared presentational component.
- If the same logic appears in more than one component, move it to a service or a pure utility function.
- Never redefine a type that already exists in `models/models.ts`.
- API base URLs are defined once per service — never repeated across methods.

---

## 11. Styling

```css
/* Define design tokens once — in :root or a global stylesheet */
:root {
  --accent: #2563eb;
  --border: #e2e8f0;
  --text-muted: #64748b;
  --font-mono: 'Fira Code', monospace;
  --radius: 6px;
  --spacing-sm: 0.5rem;
  --spacing-md: 1rem;
  --spacing-lg: 2rem;
}

/* Use tokens in component styles — never hardcoded values */
.card {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: var(--spacing-md);
  color: var(--text-muted);
}
```

**Rules:**
- All design tokens (colors, spacing, radii, font stacks) are CSS custom properties — never hardcode hex, rgb, or pixel values in component styles.
- Angular's default component style encapsulation (`ViewEncapsulation.Emulated`) is the standard — do not disable it unless you have a clear reason.
- Global styles (resets, tokens, typography) go in `styles.css`. Component-specific styles go in the component's own `.css` file.
- Do not use inline `style=""` bindings for static styles — use CSS classes.
- If an external CSS framework (e.g. Tailwind) is introduced, document the decision in `CLAUDE.md` and adopt it consistently — never mix framework classes with custom utility classes.

---

## 12. Testing

Co-locate test files next to the file under test:

```
orders-list/
├── orders-list.component.ts
└── orders-list.component.spec.ts
```

### Component Unit Tests

```typescript
describe('OrdersListComponent', () => {
  let fixture: ComponentFixture<OrdersListComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;

  beforeEach(async () => {
    orderServiceSpy = jasmine.createSpyObj('OrderService', ['getAllOrders']);
    orderServiceSpy.getAllOrders.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [OrdersListComponent],
      providers: [{ provide: OrderService, useValue: orderServiceSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(OrdersListComponent);
    fixture.detectChanges();
  });

  it('should display empty state when no orders', () => {
    expect(fixture.nativeElement.querySelector('.empty-state')).toBeTruthy();
  });
});
```

### Service Tests

```typescript
describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()]
    });
    service = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should GET all orders', () => {
    service.getAllOrders().subscribe(orders => {
      expect(orders.length).toBe(2);
    });
    const req = httpMock.expectOne('/api/v1/orders');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: '1' }, { id: '2' }]);
  });
});
```

**Rules:**
- Every component and service should have a corresponding `*.spec.ts` file.
- Use `provideHttpClientTesting()` for service tests — never make real HTTP calls in unit tests.
- Mock services in component tests using `jasmine.createSpyObj` or a manual stub — never use real services.
- Test the component's rendered output and user interactions, not its internal implementation details.
- For critical end-to-end flows (e.g. full order creation → shipment → ship), use Cypress or Playwright.
- Keep tests fast — unit tests should not spin up a full Angular module unless necessary.
