import { Component, OnInit, inject } from '@angular/core';
import { FormArray, FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { OrderService } from '../../services/order.service';
import { ApiError, Order } from '../../models/models';

@Component({
  selector: 'app-orders-list',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './orders-list.component.html',
  styleUrl: './orders-list.component.css',
})
export class OrdersListComponent implements OnInit {
  orders: Order[] = [];
  loading = true;
  errorMessage: string | null = null;

  showForm = false;
  submitting = false;
  formError: string | null = null;

  private fb = inject(FormBuilder);
  private orderService = inject(OrderService);
  private router = inject(Router);

  form = this.fb.group({
    externalOrderNumber: ['', Validators.required],
    street: ['', Validators.required],
    zipCode: ['', Validators.required],
    city: ['', Validators.required],
    country: ['DE', Validators.required],
    positions: this.fb.array([this.newPosition()]),
  });

  ngOnInit(): void {
    this.loadOrders();
  }

  get positions() {
    return this.form.get('positions') as FormArray;
  }

  newPosition() {
    return this.fb.group({
      sku: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(1)]],
      description: [''],
    });
  }

  addPosition(): void {
    this.positions.push(this.newPosition());
  }

  removePosition(index: number): void {
    if (this.positions.length > 1) {
      this.positions.removeAt(index);
    }
  }

  loadOrders(): void {
    this.loading = true;
    this.errorMessage = null;
    this.orderService.getAllOrders().subscribe({
      next: (orders) => {
        this.orders = orders;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Aufträge konnten nicht geladen werden.';
        this.loading = false;
      },
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.formError = null;

    const raw = this.form.getRawValue();
    this.orderService
      .createOrder({
        externalOrderNumber: raw.externalOrderNumber!,
        deliveryAddress: {
          street: raw.street!,
          zipCode: raw.zipCode!,
          city: raw.city!,
          country: raw.country!,
        },
        positions: (raw.positions ?? []).map((p) => ({
          sku: p.sku!,
          quantity: p.quantity!,
          description: p.description ?? undefined,
        })),
      })
      .subscribe({
        next: (order) => {
          this.submitting = false;
          this.router.navigate(['/orders', order.id]);
        },
        error: (err: HttpErrorResponse) => {
          this.submitting = false;
          const apiError = err.error as ApiError | undefined;
          this.formError = apiError?.message ?? 'Auftrag konnte nicht angelegt werden.';
        },
      });
  }
}
