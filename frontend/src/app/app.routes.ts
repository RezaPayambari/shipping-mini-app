import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'orders', pathMatch: 'full' },
  {
    path: 'orders',
    loadComponent: () => import('./pages/orders-list/orders-list.component').then((m) => m.OrdersListComponent),
  },
  {
    path: 'orders/:id',
    loadComponent: () => import('./pages/order-detail/order-detail.component').then((m) => m.OrderDetailComponent),
  },
  {
    path: 'shipments',
    loadComponent: () =>
      import('./pages/shipments-list/shipments-list.component').then((m) => m.ShipmentsListComponent),
  },
  {
    path: 'shipments/:id',
    loadComponent: () =>
      import('./pages/shipment-detail/shipment-detail.component').then((m) => m.ShipmentDetailComponent),
  },
  { path: '**', redirectTo: 'orders' },
];
