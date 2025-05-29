import { Component } from '@angular/core';
import {CommonModule} from '@angular/common';
import {NavbarComponent} from '../../layout/navbar/navbar.component';
import {SidebarComponent} from '../../layout/sidebar/sidebar.component';
import {FooterComponent} from '../../layout/footer/footer.component';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './analytics.component.html',
  styleUrls: ['./analytics.component.scss']
})
export class AnalyticsComponent {

}
