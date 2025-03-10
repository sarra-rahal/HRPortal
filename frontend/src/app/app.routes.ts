import { Routes } from '@angular/router';
import {SignUpComponent} from './auth/sign-up/sign-up.component';
import {SignInComponent} from './auth/sign-in/sign-in.component';
import {ForgotPasswordComponent} from './auth/forgot-password/forgot-password.component';
import {HrDashboardComponent} from './dashboard/hr-dashboard/hr-dashboard.component';
import {EDashboardComponent} from './dashboard/e-dashboard/e-dashboard.component';
import {authGuard} from './auth.guard';

export const routes: Routes = [
  { path: 'sign-in', component: SignInComponent },
  { path: 'sign-up', component: SignUpComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'hr-dashboard', component: HrDashboardComponent, canActivate: [authGuard]},
  { path: 'e-dashboard', component: EDashboardComponent, canActivate: [authGuard] },
  { path: '', redirectTo: '/sign-in', pathMatch: 'full' },
  { path: '**', redirectTo: '/sign-in' }
];
