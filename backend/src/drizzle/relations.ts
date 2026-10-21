import { relations } from 'drizzle-orm/relations';
import { user, request, component, requestItem, auditLog, requestEmail } from './schema';

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
  requestItems: many(requestItem),
}));

export const userRelations = relations(user, ({ many }) => ({
  requests_targetFacultyId: many(request, {
    relationName: 'request_targetFacultyId_user_id',
  }),
  requests_userId: many(request, {
    relationName: 'request_userId_user_id',
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

export const requestEmailRelations = relations(requestEmail, ({ one }) => ({
  request: one(request, {
    fields: [requestEmail.requestId],
    references: [request.id],
  }),
  recipient: one(user, {
    fields: [requestEmail.recipientId],
    references: [user.id],
  }),
}));
