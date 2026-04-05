import { relations } from 'drizzle-orm/relations';
import { user, request, component, requestItem, auditLog } from './schema';

export const requestRelations = relations(request, ({ one, many }) => ({
  user_targetFacultyId: one(user, {
    fields: [request.targetFacultyId],
    references: [user.id],
    relationName: 'request_targetFacultyId_user_id',
  }),
  user_userId: one(user, {
    fields: [request.userId],
    references: [user.id],
    relationName: 'request_userId_user_id',
  }),
  user_receivedByUserId: one(user, {
    fields: [request.receivedByUserId],
    references: [user.id],
    relationName: 'request_receivedByUserId_user_id',
  }),
  requestItems: many(requestItem),
}));

export const userRelations = relations(user, ({ many }) => ({
  requests_targetFacultyId: many(request, {
    relationName: 'request_targetFacultyId_user_id',
  }),
  requests_userId: many(request, {
    relationName: 'request_userId_user_id',
  }),
  requests_receivedByUserId: many(request, {
    relationName: 'request_receivedByUserId_user_id',
  }),
  auditLogs: many(auditLog),
}));

export const requestItemRelations = relations(requestItem, ({ one }) => ({
  component: one(component, {
    fields: [requestItem.componentId],
    references: [component.id],
  }),
  request: one(request, {
    fields: [requestItem.requestId],
    references: [request.id],
  }),
}));

export const componentRelations = relations(component, ({ many }) => ({
  requestItems: many(requestItem),
}));

export const auditLogRelations = relations(auditLog, ({ one }) => ({
  user: one(user, {
    fields: [auditLog.userId],
    references: [user.id],
  }),
}));
