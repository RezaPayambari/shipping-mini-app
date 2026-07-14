import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ShipmentService } from '../../services/shipment.service';
import { Shipment, ShipmentStatus } from '../../models/models';

@Component({
  selector: 'app-shipments-list',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './shipments-list.component.html',
  styleUrl: './shipments-list.component.css',
})
export class ShipmentsListComponent implements OnInit {
  shipments: Shipment[] = [];
  loading = true;
  errorMessage: string | null = null;

  statusFilter: ShipmentStatus | undefined = undefined;
  carrierFilter = '';

  private readonly shipmentService = inject(ShipmentService);

  ngOnInit(): void {
    this.search();
  }

  search(): void {
    this.loading = true;
    this.errorMessage = null;
    this.shipmentService.search(this.statusFilter, this.carrierFilter || undefined).subscribe({
      next: (shipments) => {
        this.shipments = shipments;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Sendungen konnten nicht geladen werden.';
        this.loading = false;
      },
    });
  }

  resetFilters(): void {
    this.statusFilter = undefined;
    this.carrierFilter = '';
    this.search();
  }
}
