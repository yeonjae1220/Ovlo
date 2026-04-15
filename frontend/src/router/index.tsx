import { createBrowserRouter } from 'react-router-dom'
import AppLayout from '../components/layout/AppLayout'
import ProtectedRoute from '../components/layout/ProtectedRoute'
import AdminRoute from '../components/layout/AdminRoute'

import LandingPage from '../pages/LandingPage'
import LoginPage from '../pages/auth/LoginPage'
import RegisterPage from '../pages/auth/RegisterPage'
import OAuthCallbackPage from '../pages/auth/OAuthCallbackPage'
import OnboardingPage from '../pages/auth/OnboardingPage'
import UniversitySearchPage from '../pages/university/UniversitySearchPage'
import ExchangeUniversitySearchPage from '../pages/university/ExchangeUniversitySearchPage'
import ExchangeUniversityDetailPage from '../pages/university/ExchangeUniversityDetailPage'
import BoardListPage from '../pages/board/BoardListPage'
import BoardDetailPage from '../pages/board/BoardDetailPage'
import CreatePostPage from '../pages/post/CreatePostPage'
import PostDetailPage from '../pages/post/PostDetailPage'
import ProfilePage from '../pages/member/ProfilePage'
import SearchPage from '../pages/member/SearchPage'
import FollowPage from '../pages/follow/FollowPage'
import ChatListPage from '../pages/chat/ChatListPage'
import ChatRoomPage from '../pages/chat/ChatRoomPage'
import AdminDashboardPage from '../pages/admin/AdminDashboardPage'
import AdminMembersPage from '../pages/admin/AdminMembersPage'

export const router = createBrowserRouter([
  // Public routes
  { path: '/', element: <LandingPage /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/universities', element: <UniversitySearchPage /> },
  { path: '/oauth/callback', element: <OAuthCallbackPage /> },

  // Public routes with AppLayout (하단바 포함, 인증 불필요)
  {
    element: <AppLayout />,
    children: [
      { path: '/exchange-universities', element: <ExchangeUniversitySearchPage /> },
      { path: '/exchange-universities/:id', element: <ExchangeUniversityDetailPage /> },
    ],
  },

  // Admin routes (ADMIN role 전용)
  {
    element: <AdminRoute />,
    children: [
      { path: '/admin', element: <AdminDashboardPage /> },
      { path: '/admin/members', element: <AdminMembersPage /> },
    ],
  },

  // Protected routes
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: '/onboarding', element: <OnboardingPage /> },
          { path: '/boards', element: <BoardListPage /> },
          { path: '/boards/:id', element: <BoardDetailPage /> },
          { path: '/posts/new', element: <CreatePostPage /> },
          { path: '/posts/:id', element: <PostDetailPage /> },
          { path: '/profile/:id', element: <ProfilePage /> },
          { path: '/search', element: <SearchPage /> },
          { path: '/follow', element: <FollowPage /> },
          { path: '/chat', element: <ChatListPage /> },
          { path: '/chat/:id', element: <ChatRoomPage /> },
        ],
      },
    ],
  },
])
