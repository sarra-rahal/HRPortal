import { Routes } from '@angular/router';
import { SignUpComponent } from './auth/sign-up/sign-up.component';
import { SignInComponent } from './auth/sign-in/sign-in.component';
import { ForgotPasswordComponent } from './auth/forgot-password/forgot-password.component';
import { WelcomeComponent } from './features/welcome/welcome.component';
import { ProfileComponent } from './features/profile/profile.component';
import { authGuard } from './core/guards/auth.guard';
import {LeaveComponent} from './features/leave/leave.component';
import {ManageLeavesComponent} from './features/manage-leaves/manage-leaves.component';

export const routes: Routes = [
  { path: 'sign-up', component: SignUpComponent },
  { path: 'sign-in', component: SignInComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'welcome', component: WelcomeComponent, canActivate: [authGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'leave', component: LeaveComponent, canActivate: [authGuard] },
  { path: 'manage-leaves', component: ManageLeavesComponent, canActivate: [authGuard] },
  // Placeholder routes for services
  { path: 'training', component: WelcomeComponent, canActivate: [authGuard] },
  { path: 'advance', component: WelcomeComponent, canActivate: [authGuard] },
  { path: 'documents', component: WelcomeComponent, canActivate: [authGuard] },
  { path: 'manage-training', component: WelcomeComponent, canActivate: [authGuard] },
  { path: 'manage-advance', component: WelcomeComponent, canActivate: [authGuard] },
  { path: 'manage-documents', component: WelcomeComponent, canActivate: [authGuard] },
  { path: 'manage-employees', component: WelcomeComponent, canActivate: [authGuard] },
  { path: '', redirectTo: '/sign-in', pathMatch: 'full' },
  { path: '**', redirectTo: '/sign-in' }
];
