import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LabelPackageRequest, Shipment, ShipmentStatus } from '../models/models';

@Injectable({ providedIn: 'root' })
export class ShipmentService {
  private readonly baseUrl = '/api/shipments';

  constructor(private http: HttpClient) {}

  getShipment(id: string): Observable<Shipment> {
    return this.http.get<Shipment>(`${this.baseUrl}/${id}`);
  }

  search(status?: ShipmentStatus | '', carrier?: string): Observable<Shipment[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    if (carrier) {
      params = params.set('carrier', carrier);
    }
    return this.http.get<Shipment[]>(this.baseUrl, { params });
  }

  labelPackage(shipmentId: string, packageId: string, request: LabelPackageRequest): Observable<Shipment> {
    return this.http.patch<Shipment>(`${this.baseUrl}/${shipmentId}/packages/${packageId}`, request);
  }

  pack(shipmentId: string): Observable<Shipment> {
    return this.http.post<Shipment>(`${this.baseUrl}/${shipmentId}/pack`, {});
  }

  ship(shipmentId: string): Observable<Shipment> {
    return this.http.post<Shipment>(`${this.baseUrl}/${shipmentId}/ship`, {});
  }
}
