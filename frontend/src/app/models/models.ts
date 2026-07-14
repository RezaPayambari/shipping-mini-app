export interface Address {
  street: string;
  zipCode: string;
  city: string;
  country: string;
}

export interface OrderPosition {
  sku: string;
  quantity: number;
  description?: string;
}

export interface ShipmentSummary {
  id: string;
  status: 'CREATED' | 'PACKED' | 'SHIPPED';
}

export interface Order {
  id: string;
  externalOrderNumber: string;
  deliveryAddress: Address;
  status: string;
  positions: OrderPosition[];
  shipment: ShipmentSummary | null;
  createdAt: string;
}

export interface CreateOrderRequest {
  externalOrderNumber: string;
  deliveryAddress: Address;
  positions: OrderPosition[];
}

export interface Package {
  id: string;
  trackingCode: string | null;
  carrier: string | null;
}

export type ShipmentStatus = 'CREATED' | 'PACKED' | 'SHIPPED';

export interface Shipment {
  id: string;
  orderId: string;
  externalOrderNumber: string;
  status: ShipmentStatus;
  shippedAt: string | null;
  packages: Package[];
}

export interface LabelPackageRequest {
  trackingCode: string;
  carrier: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}
