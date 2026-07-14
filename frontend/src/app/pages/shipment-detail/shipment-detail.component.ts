import { Component, OnInit, inject, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ShipmentService } from '../../services/shipment.service';
import { ApiError, Shipment } from '../../models/models';
import { StatusPipelineComponent } from '../../components/status-pipeline.component';

interface LabelFormValue {
  trackingCode: string;
  carrier: string;
}

@Component({
  selector: 'app-shipment-detail',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, StatusPipelineComponent, DatePipe],
  templateUrl: './shipment-detail.component.html',
  styleUrl: './shipment-detail.component.css',
})
export class ShipmentDetailComponent implements OnInit {
  readonly id = input.required<string>();

  shipment: Shipment | null = null;
  loading = true;
  errorMessage: string | null = null;
  actionError: string | null = null;
  actionPending = false;

  labelForms = new Map<string, FormGroup<{ trackingCode: any; carrier: any }>>();

  private readonly shipmentService = inject(ShipmentService);
  private readonly fb = inject(FormBuilder);

  ngOnInit(): void {
    this.load(this.id());
  }

  load(id: string): void {
    this.loading = true;
    this.shipmentService.getShipment(id).subscribe({
      next: (shipment) => {
        this.shipment = shipment;
        this.buildLabelForms(shipment);
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Sendung wurde nicht gefunden.';
        this.loading = false;
      },
    });
  }

  private buildLabelForms(shipment: Shipment): void {
    for (const pkg of shipment.packages) {
      this.labelForms.set(
        pkg.id,
        this.fb.group({
          trackingCode: [pkg.trackingCode ?? '', Validators.required],
          carrier: [pkg.carrier ?? '', Validators.required],
        }),
      );
    }
  }

  labelForm(packageId: string): FormGroup {
    return this.labelForms.get(packageId)!;
  }

  labelPackage(packageId: string): void {
    if (!this.shipment) return;
    const form = this.labelForm(packageId);
    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }
    this.actionPending = true;
    this.actionError = null;
    const raw = form.getRawValue() as LabelFormValue;

    this.shipmentService.labelPackage(this.shipment.id, packageId, raw).subscribe({
      next: (shipment) => {
        this.shipment = shipment;
        this.buildLabelForms(shipment);
        this.actionPending = false;
      },
      error: (err: HttpErrorResponse) => this.handleActionError(err),
    });
  }

  pack(): void {
    if (!this.shipment) return;
    this.actionPending = true;
    this.actionError = null;
    this.shipmentService.pack(this.shipment.id).subscribe({
      next: (shipment) => {
        this.shipment = shipment;
        this.actionPending = false;
      },
      error: (err: HttpErrorResponse) => this.handleActionError(err),
    });
  }

  ship(): void {
    if (!this.shipment) return;
    this.actionPending = true;
    this.actionError = null;
    this.shipmentService.ship(this.shipment.id).subscribe({
      next: (shipment) => {
        this.shipment = shipment;
        this.actionPending = false;
      },
      error: (err: HttpErrorResponse) => this.handleActionError(err),
    });
  }

  get canPack(): boolean {
    return this.shipment?.status === 'CREATED';
  }

  get canShip(): boolean {
    return (
      this.shipment?.status === 'PACKED' &&
      this.shipment.packages.every((p) => !!p.trackingCode)
    );
  }

  private handleActionError(err: HttpErrorResponse): void {
    this.actionPending = false;
    const apiError = err.error as ApiError | undefined;
    this.actionError = apiError?.message ?? 'Aktion konnte nicht ausgeführt werden.';
  }
}
