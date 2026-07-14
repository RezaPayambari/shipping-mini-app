import { Component, OnInit, inject, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { OrderService } from '../../services/order.service';
import { ApiError, Order } from '../../models/models';
import {ShipmentService} from "../../services/shipment.service";

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.css',
})
export class OrderDetailComponent implements OnInit {
  readonly id = input.required<string>();

  order: Order | null = null;
  loading = true;
  errorMessage: string | null = null;
  creatingShipment = false;

  private readonly router = inject(Router);
  private readonly orderService = inject(OrderService);
  private readonly shipmentService = inject(ShipmentService);

  ngOnInit(): void {
    this.load(this.id());
  }

  load(id: string): void {
    this.loading = true;
    this.orderService.getOrder(id).subscribe({
      next: (order) => {
        this.order = order;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Auftrag wurde nicht gefunden.';
        this.loading = false;
      },
    });
  }

  createShipment(): void {
    if (!this.order) return;
    this.creatingShipment = true;
    this.shipmentService.createShipment(this.order.id).subscribe({
      next: (shipment) => {
        this.creatingShipment = false;
        this.router.navigate(['/shipments', shipment.id]);
      },
      error: (err: HttpErrorResponse) => {
        this.creatingShipment = false;
        const apiError = err.error as ApiError | undefined;
        this.errorMessage = apiError?.message ?? 'Sendung konnte nicht erzeugt werden.';
      },
    });
  }
}
