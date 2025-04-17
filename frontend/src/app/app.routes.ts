import { Routes } from '@angular/router';
import { SignUpComponent } from './auth/sign-up/sign-up.component';
import { SignInComponent } from './auth/sign-in/sign-in.component';
import { ForgotPasswordComponent } from './auth/forgot-password/forgot-password.component';
import { WelcomeComponent } from './features/welcome/welcome.component';
import { ProfileComponent } from './features/profile/profile.component';
import { authGuard } from './core/guards/auth.guard';
import {LeaveComponent} from './features/leave/leave.component';
import {ManageLeavesComponent} from './features/manage-leaves/manage-leaves.component';
import {TrainingComponent} from './features/training/training.component';
import {ManageTrainingComponent} from './features/manage-training/manage-training.component';
import {ManageDocumentComponent} from './features/manage-document/manage-document.component';
import {DocumentComponent} from './features/document/document.component';
import {ResetPasswordComponent} from './auth/reset-password/reset-password.component';

export const routes: Routes = [
  { path: 'sign-up', component: SignUpComponent },
  { path: 'sign-in', component: SignInComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'welcome', component: WelcomeComponent, canActivate: [authGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'leave', component: LeaveComponent, canActivate: [authGuard] },
  { path: 'manage-leaves', component: ManageLeavesComponent, canActivate: [authGuard] },
  { path: 'training', component: TrainingComponent, canActivate: [authGuard]},
  { path: 'manage-training', component: ManageTrainingComponent, canActivate: [authGuard] },
  { path: 'manage-documents', component: ManageDocumentComponent, canActivate: [authGuard]},
  { path: 'documents', component: DocumentComponent,canActivate: [authGuard] },
  // Placeholder routes for services
  { path: 'advance', component: WelcomeComponent, canActivate: [authGuard] },
  { path: 'manage-training', component: WelcomeComponent, canActivate: [authGuard] },
  { path: 'manage-advance', component: WelcomeComponent, canActivate: [authGuard] },
  { path: 'manage-employees', component: WelcomeComponent, canActivate: [authGuard] },
  { path: '', redirectTo: '/sign-in', pathMatch: 'full' },
  { path: '**', redirectTo: '/sign-in' }
];
